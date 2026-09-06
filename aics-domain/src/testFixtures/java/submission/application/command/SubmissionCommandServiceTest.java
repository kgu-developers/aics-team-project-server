package submission.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.application.command.SubmissionArtifactInput;
import kgu.developers.domain.submission.application.command.SubmissionCommandService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.ArtifactType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException;
import kgu.developers.domain.submission.exception.SubmissionMemberConfirmationIncompleteException;
import kgu.developers.domain.submission.exception.SubmissionNotAllowedNowException;
import kgu.developers.domain.submission.exception.SubmissionNotCompletedException;
import kgu.developers.domain.submission.exception.SubmissionNotYetSubmittedException;
import kgu.developers.domain.submission.exception.SubmissionRequiredArtifactMismatchException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;

import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakeRequiredArtifactRepository;
import mock.repository.FakeSubmissionArtifactRepository;
import mock.repository.FakeSubmissionMemberConfirmationRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeSubmissionVersionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionCommandServiceTest {

    private static final Long TEAM_ID = 10L;
    private static final Long MILESTONE_ID = 5L;
    private static final Long SECTION_ID = 1L;
    private static final String USER_ID = "202699999";

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private FakeSubmissionRepository submissionRepository;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private FakeSubmissionArtifactRepository submissionArtifactRepository;
    private FakeSubmissionMemberConfirmationRepository submissionMemberConfirmationRepository;
    private FakeFileObjectRepository fileObjectRepository;
    private FakeFileStorage fileStorage;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeEnrollmentRepository enrollmentRepository;
    private FakeRequiredArtifactRepository requiredArtifactRepository;
    private FakeTeamRepository teamRepository;
    private SubmissionCommandService submissionCommandService;
    private Submission submission;

    @BeforeEach
    void setUp() {
        submissionRepository = new FakeSubmissionRepository();
        submissionVersionRepository = new FakeSubmissionVersionRepository();
        submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        submissionMemberConfirmationRepository = new FakeSubmissionMemberConfirmationRepository();
        fileObjectRepository = new FakeFileObjectRepository();
        fileStorage = new FakeFileStorage();
        teamMemberRepository = new FakeTeamMemberRepository();
        enrollmentRepository = new FakeEnrollmentRepository();
        requiredArtifactRepository = new FakeRequiredArtifactRepository();
        teamRepository = new FakeTeamRepository();

        SubmissionQueryService submissionQueryService =
                new SubmissionQueryService(submissionRepository, milestoneRepository, transactionManager);
        submissionCommandService = new SubmissionCommandService(
                submissionRepository,
                submissionVersionRepository,
                submissionArtifactRepository,
                submissionMemberConfirmationRepository,
                fileObjectRepository,
                fileStorage,
                milestoneRepository,
                teamMemberRepository,
                enrollmentRepository,
                requiredArtifactRepository,
                teamRepository,
                submissionQueryService
        );

        submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
    }

    @Test
    @DisplayName("제출하면 버전이 1부터 시작하고 제출의 currentVersion·status가 갱신된다")
    void submitVersion_CreatesFirstVersion() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "1차 제출", null, List.of());

        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.isLate()).isFalse();

        Submission updated = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(updated.getCurrentVersion()).isEqualTo(1);
        assertThat(updated.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("재제출하면 버전 번호가 이어서 올라간다")
    void submitVersion_IncrementsVersionOnResubmit() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));

        submissionCommandService.submitVersion(submission.getId(), USER_ID, "1차", null, List.of());
        SubmissionVersion second = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "2차", "오타 수정", List.of());

        assertThat(second.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("마감이 지나 지각제출기간도 없으면 제출을 거부한다")
    void submitVersion_RejectsWhenNotAllowedNow() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Milestone closedMilestone = Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, past, null, null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(closedMilestone));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "지각 제출 시도", null, List.of()))
                .isInstanceOf(SubmissionNotAllowedNowException.class);
    }

    @Test
    @DisplayName("아직 opensAt 전이면 공개된 마일스톤이라도 제출을 거부한다")
    void submitVersion_RejectsBeforeOpensAt() {
        LocalDateTime opensAt = LocalDateTime.now().plusDays(1);
        LocalDateTime dueAt = LocalDateTime.now().plusDays(7);
        Milestone notYetOpenMilestone = Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(opensAt, dueAt, null, null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(notYetOpenMilestone));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(notYetOpenMilestone));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "너무 이른 제출", null, List.of()))
                .isInstanceOf(SubmissionNotAllowedNowException.class);
    }

    @Test
    @DisplayName("DRAFT 상태 마일스톤은 기간이 열려 있어도 제출을 거부한다")
    void submitVersion_RejectsWhenMilestoneIsDraft() {
        Milestone draftMilestone = Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.DRAFT,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(draftMilestone));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "초안 상태 제출", null, List.of()))
                .isInstanceOf(SubmissionNotAllowedNowException.class);
    }

    @Test
    @DisplayName("마감이 지나면 그 버전은 지각으로 기록된다")
    void submitVersion_MarksLateAfterDueDate() {
        LocalDateTime due = LocalDateTime.now().minusHours(1);
        LocalDateTime lateUntil = LocalDateTime.now().plusDays(1);
        Milestone milestone = Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, due, lateUntil, null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone));

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "지각 제출", null, List.of());

        assertThat(version.isLate()).isTrue();
    }

    @Test
    @DisplayName("FILE 아티팩트는 업로드 후 FileObject로 저장되고 SubmissionArtifact가 그 파일을 참조한다")
    void submitVersion_UploadsFileArtifact() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        MockMultipartFile file = new MockMultipartFile("file", "발표자료.pdf", "application/pdf", "content".getBytes());

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "발표자료 제출", null,
                List.of(new SubmissionArtifactInput(null, ArtifactType.FILE, file, null, null)));

        List<SubmissionArtifact> artifacts = submissionArtifactRepository.findAllByVersionId(version.getId());
        assertThat(artifacts).hasSize(1);
        SubmissionArtifact artifact = artifacts.get(0);
        assertThat(artifact.getType()).isEqualTo(ArtifactType.FILE);
        assertThat(artifact.getFileId()).isNotNull();

        FileObject fileObject = fileObjectRepository.findById(artifact.getFileId()).orElseThrow();
        assertThat(fileObject.getFileName()).isEqualTo("발표자료.pdf");
        assertThat(fileObject.getUploadedBy()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("LINK 아티팩트는 URL을 그대로 저장한다")
    void submitVersion_SavesLinkArtifact() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "링크 제출", null,
                List.of(new SubmissionArtifactInput(null, ArtifactType.LINK, null, "https://github.com/example", null)));

        SubmissionArtifact artifact = submissionArtifactRepository.findAllByVersionId(version.getId()).get(0);
        assertThat(artifact.getType()).isEqualTo(ArtifactType.LINK);
        assertThat(artifact.getUrl()).isEqualTo("https://github.com/example");
    }

    @Test
    @DisplayName("CHEERPJ_RUN 아티팩트는 타입이 TEXT로 바뀌지 않고 URL이 그대로 저장된다")
    void submitVersion_SavesCheerpjRunArtifact() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "CheerpJ 실행 링크 제출", null,
                List.of(new SubmissionArtifactInput(null, ArtifactType.CHEERPJ_RUN, null, "https://cheerpj.example/run", null)));

        SubmissionArtifact artifact = submissionArtifactRepository.findAllByVersionId(version.getId()).get(0);
        assertThat(artifact.getType()).isEqualTo(ArtifactType.CHEERPJ_RUN);
        assertThat(artifact.getUrl()).isEqualTo("https://cheerpj.example/run");
    }

    @Test
    @DisplayName("필수 산출물이 LINK인데 URL이 비어있으면 제출이 거부된다")
    void submitVersion_RejectsBlankUrlForRequiredLink() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        RequiredArtifact linkArtifact = requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID, RequiredArtifactType.LINK, "발표자료 링크", true, null, null));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "빈 URL 제출", null,
                List.of(new SubmissionArtifactInput(linkArtifact.getId(), ArtifactType.LINK, null, "  ", null))))
                .isInstanceOf(SubmissionRequiredArtifactMismatchException.class);
    }

    @Test
    @DisplayName("필수 산출물이 TEXT인데 본문이 비어있으면 제출이 거부된다")
    void submitVersion_RejectsBlankContentForRequiredText() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        RequiredArtifact textArtifact = requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID, RequiredArtifactType.TEXT, "변경 내용", true, null, null));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "빈 본문 제출", null,
                List.of(new SubmissionArtifactInput(textArtifact.getId(), ArtifactType.TEXT, null, null, null))))
                .isInstanceOf(SubmissionRequiredArtifactMismatchException.class);
    }

    @Test
    @DisplayName("필수 산출물을 빠뜨리면 제출이 거부된다")
    void submitVersion_RejectsWhenRequiredArtifactMissing() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID, RequiredArtifactType.LINK, "발표자료 링크", true, null, null));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "필수 산출물 없이 제출", null, List.of()))
                .isInstanceOf(SubmissionRequiredArtifactMismatchException.class);
    }

    @Test
    @DisplayName("다른 마일스톤 소속 requiredArtifactId는 거부된다")
    void submitVersion_RejectsRequiredArtifactFromAnotherMilestone() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        RequiredArtifact otherMilestoneArtifact = requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID + 1, RequiredArtifactType.LINK, "다른 마일스톤 링크", false, null, null));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "잘못된 산출물 참조", null,
                List.of(new SubmissionArtifactInput(otherMilestoneArtifact.getId(), ArtifactType.LINK,
                        null, "https://github.com/example", null))))
                .isInstanceOf(SubmissionRequiredArtifactMismatchException.class);
    }

    @Test
    @DisplayName("요구 산출물의 타입과 다른 타입으로 제출하면 거부된다")
    void submitVersion_RejectsArtifactTypeMismatch() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        RequiredArtifact linkArtifact = requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID, RequiredArtifactType.LINK, "발표자료 링크", true, null, null));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "타입 불일치", null,
                List.of(new SubmissionArtifactInput(linkArtifact.getId(), ArtifactType.TEXT, null, null, "링크 대신 텍스트"))))
                .isInstanceOf(SubmissionRequiredArtifactMismatchException.class);
    }

    @Test
    @DisplayName("허용 확장자를 벗어난 파일은 거부된다")
    void submitVersion_RejectsDisallowedFileExtension() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        RequiredArtifact fileArtifact = requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID, RequiredArtifactType.FILE, "보고서", true, "pdf", null));
        MockMultipartFile wrongExtensionFile =
                new MockMultipartFile("file", "보고서.hwp", "application/octet-stream", "content".getBytes());

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "잘못된 확장자", null,
                List.of(new SubmissionArtifactInput(fileArtifact.getId(), ArtifactType.FILE, wrongExtensionFile, null, null))))
                .isInstanceOf(SubmissionRequiredArtifactMismatchException.class);
    }

    @Test
    @DisplayName("필수 FILE 산출물에 0바이트 파일을 올리면 거부된다")
    void submitVersion_RejectsEmptyRequiredFile() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        RequiredArtifact fileArtifact = requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID, RequiredArtifactType.FILE, "보고서", true, null, null));
        MockMultipartFile emptyFile = new MockMultipartFile("file", "보고서.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "빈 파일 제출", null,
                List.of(new SubmissionArtifactInput(fileArtifact.getId(), ArtifactType.FILE, emptyFile, null, null))))
                .isInstanceOf(SubmissionRequiredArtifactMismatchException.class);
    }

    @Test
    @DisplayName("필수 산출물을 모두 채우면 제출이 성공한다")
    void submitVersion_SucceedsWhenAllRequiredArtifactsPresent() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        RequiredArtifact linkArtifact = requiredArtifactRepository.save(RequiredArtifact.create(
                MILESTONE_ID, RequiredArtifactType.LINK, "발표자료 링크", true, null, null));

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "필수 산출물 포함 제출", null,
                List.of(new SubmissionArtifactInput(linkArtifact.getId(), ArtifactType.LINK,
                        null, "https://github.com/example", null)));

        assertThat(version.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("본인 확인을 등록하면 나중에 다시 등록해도(멱등) 하나의 행으로 남는다")
    void confirmAsMember_IsIdempotent() {
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);

        List<SubmissionMemberConfirmation> confirmations =
                submissionMemberConfirmationRepository.findAllBySubmissionId(submission.getId());
        assertThat(confirmations).hasSize(1);
        assertThat(confirmations.get(0).confirmsVersion(submission.getCurrentVersion())).isTrue();
    }

    @Test
    @DisplayName("확인을 취소하면(멱등) 그 행이 없어지고, 확인한 적 없어도 취소는 그대로 성공한다")
    void cancelConfirmation_RemovesRecordAndIsIdempotent() {
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);

        submissionCommandService.cancelConfirmation(submission.getId(), USER_ID);
        assertThat(submissionMemberConfirmationRepository.findAllBySubmissionId(submission.getId())).isEmpty();

        assertThat(catchThrowable(
                () -> submissionCommandService.cancelConfirmation(submission.getId(), USER_ID))).isNull();
    }

    @Test
    @DisplayName("완료 처리하면 아직 제출한 적 없는 상태에서는 거부된다")
    void completeSubmission_RejectsWhenNotYetSubmitted() {
        assertThatThrownBy(() -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))
                .isInstanceOf(SubmissionNotYetSubmittedException.class);
    }

    @Test
    @DisplayName("완료 처리하면 상태와 완료 시각·완료자가 실제로 저장된다")
    void completeSubmission_PersistsCompletedState() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "제출", null, List.of());

        submissionCommandService.completeSubmission(submission.getId(), USER_ID);

        Submission updated = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubmissionStatus.COMPLETED);
        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getCompletedAt()).isNotNull();
        assertThat(updated.getCompletedBy()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("최종보고서가 아닌 마일스톤은 팀원 확인 없이도 완료 처리된다")
    void completeSubmission_SkipsGateForNonFinalReport() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "제출", null, List.of());

        assertThat(catchThrowable(
                () -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))).isNull();
    }

    @Test
    @DisplayName("최종보고서 마일스톤은 활성 팀원 전원이 확인해야 완료 처리된다")
    void completeSubmission_RequiresAllActiveMembersConfirmed() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, USER_ID, true, "팀장"));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202611111", false, "팀원"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202611111", Role.STUDENT, Status.ACTIVE));
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "최종보고서 제출", null, List.of());

        assertThatThrownBy(() -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))
                .isInstanceOf(SubmissionMemberConfirmationIncompleteException.class);

        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);
        submissionCommandService.confirmAsMember(submission.getId(), "202611111");

        assertThat(catchThrowable(
                () -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))).isNull();
    }

    @Test
    @DisplayName("확인을 취소하면 다시 완료 게이트를 통과하지 못한다")
    void completeSubmission_RejectsAfterConfirmationCancelled() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, USER_ID, true, "팀장"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "최종보고서 제출", null, List.of());
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);

        submissionCommandService.cancelConfirmation(submission.getId(), USER_ID);

        assertThatThrownBy(() -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))
                .isInstanceOf(SubmissionMemberConfirmationIncompleteException.class);
    }

    @Test
    @DisplayName("재제출로 버전이 올라가면 이전 버전에서 한 확인은 더 이상 유효하지 않다")
    void completeSubmission_InvalidatesConfirmationFromPreviousVersion() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, USER_ID, true, "팀장"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "1차 제출", null, List.of());
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);

        // 재제출로 currentVersion이 올라감 — 1차 버전에 대한 확인은 2차 버전을 확인한 게 아니다.
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "2차 제출", null, List.of());

        assertThatThrownBy(() -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))
                .isInstanceOf(SubmissionMemberConfirmationIncompleteException.class);
    }

    @Test
    @DisplayName("최종보고서 완료 게이트는 탈퇴한 팀원은 확인 대상에서 뺀다")
    void completeSubmission_ExcludesWithdrawnMember() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, USER_ID, true, "팀장"));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202611111", false, "탈퇴함"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202611111", Role.STUDENT, Status.WITHDRAWN));
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "최종보고서 제출", null, List.of());

        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);

        assertThat(catchThrowable(
                () -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))).isNull();
    }

    @Test
    @DisplayName("최종보고서 완료 게이트는 활성 조교도 확인 대상에서 뺀다(활성 STUDENT만 대상)")
    void completeSubmission_ExcludesActiveAssistant() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, USER_ID, true, "팀장"));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202611111", false, "조교"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202611111", Role.ASSISTANT, Status.ACTIVE));
        submissionCommandService.submitVersion(submission.getId(), USER_ID, "최종보고서 제출", null, List.of());

        // 조교(202611111)는 확인을 아예 안 했지만, 활성 STUDENT(USER_ID)만 확인하면 게이트를 통과해야 한다.
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID);

        assertThat(catchThrowable(
                () -> submissionCommandService.completeSubmission(submission.getId(), USER_ID))).isNull();
    }

    @Test
    @DisplayName("완료되지 않은 제출은 재오픈할 수 없다")
    void reopenSubmission_RejectsWhenNotCompleted() {
        assertThatThrownBy(() -> submissionCommandService.reopenSubmission(
                submission.getId(), "202400001", LocalDateTime.now().plusDays(1)))
                .isInstanceOf(SubmissionNotCompletedException.class);
    }

    @Test
    @DisplayName("교수가 재오픈하면 그 시각까지 재제출이 허용된다")
    void reopenSubmission_AllowsResubmissionUntilRevisionDueAt() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Milestone closedMilestone = Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, past, null, null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(closedMilestone));

        // 완료된 제출만 재오픈 대상이라, 완료된 상태를 먼저 만들어둔다(마감이 지나 실제 API로는
        // 더 이상 제출을 못 하므로, 완료 처리 대신 상태를 직접 만들어 재오픈 시나리오만 검증).
        submission.recordNewVersion(1);
        submission.complete(USER_ID);
        submissionRepository.save(submission);

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "재오픈 전", null, List.of()))
                .isInstanceOf(SubmissionNotAllowedNowException.class);

        submissionCommandService.reopenSubmission(submission.getId(), "202400001", LocalDateTime.now().plusDays(1));

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "재오픈 후 재제출", null, List.of());
        assertThat(version.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("발표순서는 분반의 팀 전체를 대상으로 하고, 아직 조회 안 해본 팀도 생성해서 반영한다")
    void assignPresentationOrders_CoversAllSectionTeams() {
        Team teamA = teamRepository.save(Team.create(SECTION_ID, "A팀", null, null, null));
        Team teamB = teamRepository.save(Team.create(SECTION_ID, "B팀", null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        submissionCommandService.assignPresentationOrders(
                MILESTONE_ID, Map.of(teamA.getId(), 2, teamB.getId(), 1));

        Submission submissionA = submissionRepository.findByTeamIdAndMilestoneId(teamA.getId(), MILESTONE_ID).orElseThrow();
        Submission submissionB = submissionRepository.findByTeamIdAndMilestoneId(teamB.getId(), MILESTONE_ID).orElseThrow();
        assertThat(submissionA.getPresentationOrder()).isEqualTo(2);
        assertThat(submissionB.getPresentationOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("분반의 일부 팀이 발표순서 지정에서 빠지면 거부된다")
    void assignPresentationOrders_RejectsWhenTeamMissing() {
        Team teamA = teamRepository.save(Team.create(SECTION_ID, "A팀", null, null, null));
        teamRepository.save(Team.create(SECTION_ID, "B팀", null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        assertThatThrownBy(() -> submissionCommandService.assignPresentationOrders(
                MILESTONE_ID, Map.of(teamA.getId(), 1)))
                .isInstanceOf(SubmissionInvalidPresentationOrderException.class);
    }

    @Test
    @DisplayName("발표순서가 중복되면 거부된다")
    void assignPresentationOrders_RejectsDuplicateOrder() {
        Team teamA = teamRepository.save(Team.create(SECTION_ID, "A팀", null, null, null));
        Team teamB = teamRepository.save(Team.create(SECTION_ID, "B팀", null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        assertThatThrownBy(() -> submissionCommandService.assignPresentationOrders(
                MILESTONE_ID, Map.of(teamA.getId(), 1, teamB.getId(), 1)))
                .isInstanceOf(SubmissionInvalidPresentationOrderException.class);
    }

    @Test
    @DisplayName("발표순서에 0 이하 값이 있으면 거부된다")
    void assignPresentationOrders_RejectsNonPositiveOrder() {
        Team teamA = teamRepository.save(Team.create(SECTION_ID, "A팀", null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));

        assertThatThrownBy(() -> submissionCommandService.assignPresentationOrders(
                MILESTONE_ID, Map.of(teamA.getId(), 0)))
                .isInstanceOf(SubmissionInvalidPresentationOrderException.class);
    }

    private Milestone openMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
    }

    private Milestone finalReportMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "최종보고서", null, 5, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
                MilestoneType.FINAL_REPORT);
    }
}
