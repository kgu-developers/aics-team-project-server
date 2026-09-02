package submission.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
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
import kgu.developers.domain.submission.exception.SubmissionMemberConfirmationIncompleteException;
import kgu.developers.domain.submission.exception.SubmissionNotAllowedNowException;
import kgu.developers.domain.teamMember.domain.TeamMember;

import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakeSubmissionArtifactRepository;
import mock.repository.FakeSubmissionMemberConfirmationRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeSubmissionVersionRepository;
import mock.repository.FakeTeamMemberRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionCommandServiceTest {

    private static final Long TEAM_ID = 10L;
    private static final Long MILESTONE_ID = 5L;
    private static final Long SECTION_ID = 1L;
    private static final String USER_ID = "202699999";

    @Mock
    private MilestoneRepository milestoneRepository;

    private FakeSubmissionRepository submissionRepository;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private FakeSubmissionArtifactRepository submissionArtifactRepository;
    private FakeSubmissionMemberConfirmationRepository submissionMemberConfirmationRepository;
    private FakeFileObjectRepository fileObjectRepository;
    private FakeFileStorage fileStorage;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeEnrollmentRepository enrollmentRepository;
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

        SubmissionQueryService submissionQueryService =
                new SubmissionQueryService(submissionRepository, milestoneRepository);
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
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(closedMilestone));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "지각 제출 시도", null, List.of()))
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
                List.of(new SubmissionArtifactInput(1L, ArtifactType.FILE, file, null, null)));

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
                List.of(new SubmissionArtifactInput(2L, ArtifactType.LINK, null, "https://github.com/example", null)));

        SubmissionArtifact artifact = submissionArtifactRepository.findAllByVersionId(version.getId()).get(0);
        assertThat(artifact.getType()).isEqualTo(ArtifactType.LINK);
        assertThat(artifact.getUrl()).isEqualTo("https://github.com/example");
    }

    @Test
    @DisplayName("본인 확인을 등록하면 나중에 다시 등록해도 하나로 덮어써진다")
    void confirmAsMember_UpsertsSingleRecord() {
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID, true, false, "1차");
        submissionCommandService.confirmAsMember(submission.getId(), USER_ID, true, true, "수정함");

        List<SubmissionMemberConfirmation> confirmations =
                submissionMemberConfirmationRepository.findAllBySubmissionId(submission.getId());
        assertThat(confirmations).hasSize(1);
        assertThat(confirmations.get(0).isConfirmedArtifacts()).isTrue();
        assertThat(confirmations.get(0).getOneLineReview()).isEqualTo("수정함");
    }

    @Test
    @DisplayName("최종보고서가 아닌 마일스톤은 팀원 확인 없이도 완료 처리된다")
    void completeSubmission_SkipsGateForNonFinalReport() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> submissionCommandService.completeSubmission(submission.getId()))).isNull();
    }

    @Test
    @DisplayName("최종보고서 마일스톤은 활성 팀원 전원이 확인해야 완료 처리된다")
    void completeSubmission_RequiresAllActiveMembersConfirmed() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, USER_ID, true, "팀장"));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202611111", false, "팀원"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202611111", Role.STUDENT, Status.ACTIVE));

        assertThatThrownBy(() -> submissionCommandService.completeSubmission(submission.getId()))
                .isInstanceOf(SubmissionMemberConfirmationIncompleteException.class);

        submissionCommandService.confirmAsMember(submission.getId(), USER_ID, true, true, null);
        submissionCommandService.confirmAsMember(submission.getId(), "202611111", true, true, null);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> submissionCommandService.completeSubmission(submission.getId()))).isNull();
    }

    @Test
    @DisplayName("최종보고서 완료 게이트는 탈퇴한 팀원은 확인 대상에서 뺀다")
    void completeSubmission_ExcludesWithdrawnMember() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(finalReportMilestone()));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, USER_ID, true, "팀장"));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202611111", false, "탈퇴함"));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, "202611111", Role.STUDENT, Status.WITHDRAWN));

        submissionCommandService.confirmAsMember(submission.getId(), USER_ID, true, true, null);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> submissionCommandService.completeSubmission(submission.getId()))).isNull();
    }

    @Test
    @DisplayName("교수가 재오픈하면 그 시각까지 재제출이 허용된다")
    void reopenSubmission_AllowsResubmissionUntilRevisionDueAt() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Milestone closedMilestone = Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, past, null, null, null, null));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(closedMilestone));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(closedMilestone));

        assertThatThrownBy(() -> submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "재오픈 전", null, List.of()))
                .isInstanceOf(SubmissionNotAllowedNowException.class);

        submissionCommandService.reopenSubmission(submission.getId(), "202400001", LocalDateTime.now().plusDays(1));

        SubmissionVersion version = submissionCommandService.submitVersion(
                submission.getId(), USER_ID, "재오픈 후 재제출", null, List.of());
        assertThat(version.getVersion()).isEqualTo(1);
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
