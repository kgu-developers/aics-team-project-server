package submission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.api.submission.application.SubmissionFacade;
import kgu.developers.api.submission.presentation.request.PresentationContentRequest;
import kgu.developers.api.submission.presentation.request.PresentationOrderRequest;
import kgu.developers.api.submission.presentation.request.SubmissionArtifactRequest;
import kgu.developers.api.submission.presentation.request.SubmissionReopenRequest;
import kgu.developers.api.submission.presentation.response.MilestonePresentationsResponse;
import kgu.developers.api.submission.presentation.response.PresentationContentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionMemberConsentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.presentationcontent.application.command.PresentationContentCommandService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.command.SubmissionCommandService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.exception.SubmissionLeaderOnlyException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;

import mock.repository.FakeEditLockRepository;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakePresentationContentRepository;
import mock.repository.FakeRequiredArtifactRepository;
import mock.repository.FakeSubmissionArtifactRepository;
import mock.repository.FakeSubmissionMemberConfirmationRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeSubmissionVersionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;

class SubmissionFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final Long MILESTONE_ID = 5L;
    private static final String LEADER = "202688888";
    private static final String MEMBER = "202699999";
    private static final String NON_MEMBER = "202600000";
    private static final String PROFESSOR = "professor1";

    private MilestoneRepository milestoneRepository;
    private SectionQueryService sectionQueryService;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeSubmissionRepository submissionRepository;
    private FakeTeamRepository teamRepository;
    private FakeEditLockRepository editLockRepository;
    private FakeFileObjectRepository fileObjectRepository;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private FakeSubmissionArtifactRepository submissionArtifactRepository;
    private SubmissionFacade submissionFacade;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        sectionQueryService = mock(SectionQueryService.class);

        teamMemberRepository = new FakeTeamMemberRepository();
        teamMemberRepository.save(TeamMember.create(TEAM_ID, LEADER, true, "팀장"));
        teamMemberRepository.save(TeamMember.create(TEAM_ID, MEMBER, false, "백엔드"));
        teamMemberRepository.assignTeamToSection(TEAM_ID, SECTION_ID);

        submissionRepository = new FakeSubmissionRepository();
        submissionVersionRepository = new FakeSubmissionVersionRepository();
        submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        FakeSubmissionMemberConfirmationRepository submissionMemberConfirmationRepository =
                new FakeSubmissionMemberConfirmationRepository();
        FakePresentationContentRepository presentationContentRepository = new FakePresentationContentRepository();
        fileObjectRepository = new FakeFileObjectRepository();
        FakeFileStorage fileStorage = new FakeFileStorage();
        FakeEnrollmentRepository enrollmentRepository = new FakeEnrollmentRepository();
        enrollmentRepository.save(Enrollment.create(SECTION_ID, LEADER, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.ACTIVE));
        FakeRequiredArtifactRepository requiredArtifactRepository = new FakeRequiredArtifactRepository();
        teamRepository = new FakeTeamRepository();
        editLockRepository = new FakeEditLockRepository();
        EditLockQueryService editLockQueryService = new EditLockQueryService(editLockRepository);

        SubmissionQueryService submissionQueryService =
                new SubmissionQueryService(submissionRepository, milestoneRepository, mock(org.springframework.transaction.PlatformTransactionManager.class));
        SubmissionCommandService submissionCommandService = new SubmissionCommandService(
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
        PresentationContentCommandService presentationContentCommandService =
                new PresentationContentCommandService(presentationContentRepository);

        submissionFacade = new SubmissionFacade(
                submissionCommandService,
                submissionQueryService,
                submissionVersionRepository,
                submissionArtifactRepository,
                submissionMemberConfirmationRepository,
                presentationContentRepository,
                presentationContentCommandService,
                fileObjectRepository,
                fileStorage,
                milestoneRepository,
                teamMemberRepository,
                enrollmentRepository,
                editLockQueryService,
                sectionQueryService
        );
    }

    @Test
    @DisplayName("처음 조회하는 팀원은 not_submitted 상태의 제출을 자동으로 받는다")
    void getMyTeamSubmission_CreatesLazilyForFirstView() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionResponse response = submissionFacade.getMyTeamSubmission(MILESTONE_ID, MEMBER);

        assertThat(response.status()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
        assertThat(response.teamId()).isEqualTo(TEAM_ID);
    }

    @Test
    @DisplayName("그 분반에 팀이 없는 사용자는 우리팀 제출 조회를 할 수 없다")
    void getMyTeamSubmission_RejectsNonSectionMember() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getMyTeamSubmission(MILESTONE_ID, NON_MEMBER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("다른 팀 소속은 제출 상세를 조회할 수 없다")
    void getSubmission_RejectsNonTeamMember() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getSubmission(submission.getId(), NON_MEMBER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀원은 제출 상세를 조회할 수 있다")
    void getSubmission_AllowsTeamMember() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionResponse response = submissionFacade.getSubmission(submission.getId(), MEMBER);

        assertThat(response.id()).isEqualTo(submission.getId());
    }

    @Test
    @DisplayName("탈퇴한 사용자는 팀원 행이 남아있어도 비공개 제출 상세를 조회할 수 없다")
    void getSubmission_RejectsWithdrawnEnrollment() {
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202677776", false, "탈퇴예정"));
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getSubmission(submission.getId(), "202677776"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("제출하면 currentVersion이 1로 올라간 응답을 받는다")
    void submitVersion_ReturnsUpdatedSubmission() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionResponse response = submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, List.of(), List.of(), List.of());

        assertThat(response.currentVersion()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("아티팩트 종류(type)가 없으면 NPE 대신 400으로 거부된다")
    void submitVersion_RejectsMissingArtifactType() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        SubmissionArtifactRequest missingType = new SubmissionArtifactRequest(null, null, null, null);

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, List.of(missingType), List.of(), List.of()))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionArtifactTypeRequiredException.class);
    }

    @Test
    @DisplayName("아티팩트 배열 원소 자체가 null이면 NPE 대신 400으로 거부된다")
    void submitVersion_RejectsNullArtifactElement() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        List<SubmissionArtifactRequest> artifactsWithNullElement = new java.util.ArrayList<>();
        artifactsWithNullElement.add(null);

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), MEMBER, "1차 제출", null, artifactsWithNullElement, List.of(), List.of()))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionArtifactTypeRequiredException.class);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 팀원 행이 남아있어도 제출할 수 없다")
    void submitVersion_RejectsWithdrawnEnrollment() {
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202677777", false, "탈퇴예정"));
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), "202677777", "탈퇴 후 제출 시도", null, List.of(), List.of(), List.of()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀장이 아니면 완료 처리를 할 수 없다")
    void completeSubmission_RejectsNonLeader() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.completeSubmission(submission.getId(), MEMBER))
                .isInstanceOf(SubmissionLeaderOnlyException.class);
    }

    @Test
    @DisplayName("일반 마일스톤은 이미 제출한 팀장이면 팀원 확인 없이도 완료 게이트를 통과한다")
    void completeSubmission_AllowsLeaderForGeneralMilestone() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        submissionFacade.submitVersion(submission.getId(), LEADER, "제출", null, List.of(), List.of(), List.of());

        SubmissionResponse response = submissionFacade.completeSubmission(submission.getId(), LEADER);

        assertThat(response.status()).isEqualTo(SubmissionStatus.COMPLETED);
    }

    @Test
    @DisplayName("탈퇴했거나 조교로 전환된 기존 팀장은 완료 처리를 할 수 없다")
    void completeSubmission_RejectsLeaderWithoutActiveStudentEnrollment() {
        String withdrawnLeader = "202677778";
        teamMemberRepository.save(TeamMember.create(TEAM_ID, withdrawnLeader, true, "탈퇴한 팀장"));
        // 활성 STUDENT enrollment를 일부러 안 심어둠(탈퇴했거나 조교로 전환된 상황을 흉내).
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.completeSubmission(submission.getId(), withdrawnLeader))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("아직 제출한 적 없으면 팀장이어도 완료 처리할 수 없다")
    void completeSubmission_RejectsWhenNotYetSubmitted() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.completeSubmission(submission.getId(), LEADER))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionNotYetSubmittedException.class);
    }

    @Test
    @DisplayName("담당 교수가 아니면 재오픈할 수 없다")
    void reopenSubmission_RejectsNonOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> submissionFacade.reopenSubmission(
                submission.getId(), PROFESSOR, new SubmissionReopenRequest(LocalDateTime.now().plusDays(1))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionAccessDeniedException.class);
    }

    @Test
    @DisplayName("완료된 제출은 담당 교수가 재오픈할 수 있다")
    void reopenSubmission_AllowsOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        submission.recordNewVersion(1);
        submission.complete(LEADER);
        submissionRepository.save(submission);
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionResponse response = submissionFacade.reopenSubmission(
                submission.getId(), PROFESSOR, new SubmissionReopenRequest(LocalDateTime.now().plusDays(1)));

        assertThat(response.status()).isEqualTo(SubmissionStatus.REVISION_REQUESTED);
        // 재오픈 후엔 "미완료면 null"이라는 응답 계약대로 완료 이력이 지워져야 한다.
        assertThat(response.completedAt()).isNull();
        assertThat(response.completedBy()).isNull();
    }

    @Test
    @DisplayName("완료되지 않은 제출은 담당 교수여도 재오픈할 수 없다")
    void reopenSubmission_RejectsWhenNotCompletedEvenForOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionFacade.reopenSubmission(
                submission.getId(), PROFESSOR, new SubmissionReopenRequest(LocalDateTime.now().plusDays(1))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionNotCompletedException.class);
    }

    @Test
    @DisplayName("다른 팀 소속은 발표 공개자료를 수정할 수 없다")
    void updatePresentationContent_RejectsNonTeamMember() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));

        assertThatThrownBy(() -> submissionFacade.updatePresentationContent(
                submission.getId(), NON_MEMBER, new PresentationContentRequest("소개", null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀원은 발표 공개자료를 작성할 수 있고, 팀 소속이 아니어도 조회는 가능하다")
    void updateAndGetPresentationContent_TeamWriteAnyoneRead() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));

        PresentationContentResponse updated = submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, null, "https://youtube.com/x"));
        assertThat(updated.introText()).isEqualTo("소개");

        PresentationContentResponse fetched = submissionFacade.getPresentationContent(submission.getId(), NON_MEMBER);
        assertThat(fetched.introText()).isEqualTo("소개");
    }

    @Test
    @DisplayName("다른 사람이 편집잠금을 쥐고 있으면 발표 공개자료를 수정할 수 없다")
    void updatePresentationContent_RejectsWhenLockedByAnother() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));

        new EditLockCommandService(editLockRepository).acquire(
                kgu.developers.domain.editlock.domain.EditLockTargetType.PRESENTATION_CONTENT, submission.getId(), LEADER);

        assertThatThrownBy(() -> submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, null, null)))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionAccessDeniedException.class);
    }

    @Test
    @DisplayName("담당 교수는 발표순서를 일괄 지정할 수 있고, 지정된 순서대로 목록이 조회된다")
    void assignPresentationOrder_ThenListedInOrder() {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("우리팀").build());
        teamRepository.save(Team.builder().id(20L).sectionId(SECTION_ID).name("다른팀").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        submissionFacade.assignPresentationOrder(MILESTONE_ID, PROFESSOR, new PresentationOrderRequest(List.of(
                new PresentationOrderRequest.TeamOrder(TEAM_ID, 2),
                new PresentationOrderRequest.TeamOrder(20L, 1)
        )));

        MilestonePresentationsResponse response = submissionFacade.getMilestonePresentations(MILESTONE_ID, MEMBER);
        assertThat(response.contents()).hasSize(2);
        assertThat(response.contents().get(0).teamId()).isEqualTo(20L);
        assertThat(response.contents().get(1).teamId()).isEqualTo(TEAM_ID);
    }

    @Test
    @DisplayName("분반의 일부 팀이 빠지면 발표순서 지정이 거부된다")
    void assignPresentationOrder_RejectsWhenTeamMissing() {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("우리팀").build());
        teamRepository.save(Team.builder().id(20L).sectionId(SECTION_ID).name("다른팀").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionFacade.assignPresentationOrder(
                MILESTONE_ID, PROFESSOR, new PresentationOrderRequest(List.of(
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 1)))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException.class);
    }

    @Test
    @DisplayName("발표순서 지정 요청에 팀이 중복되면 거부된다")
    void assignPresentationOrder_RejectsDuplicateTeam() {
        teamRepository.save(Team.builder().id(TEAM_ID).sectionId(SECTION_ID).name("우리팀").build());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionFacade.assignPresentationOrder(
                MILESTONE_ID, PROFESSOR, new PresentationOrderRequest(List.of(
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 1),
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 2)))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException.class);
    }

    @Test
    @DisplayName("완료된 제출은 공식 기간이 남아있어도 재오픈 전에는 재제출할 수 없다")
    void submitVersion_RejectsWhenAlreadyCompleted() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        submissionFacade.submitVersion(submission.getId(), LEADER, "1차 제출", null, List.of(), List.of(), List.of());
        submissionFacade.completeSubmission(submission.getId(), LEADER);

        assertThatThrownBy(() -> submissionFacade.submitVersion(
                submission.getId(), MEMBER, "완료 후 재제출 시도", null, List.of(), List.of(), List.of()))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionNotAllowedNowException.class);
    }

    @Test
    @DisplayName("발표 타입이 아닌 마일스톤에는 발표 공개자료를 조회·수정할 수 없다")
    void updatePresentationContent_RejectsNonPresentationMilestone() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, null, null)))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionMilestoneTypeMismatchException.class);
    }

    @Test
    @DisplayName("우리 제출물에 첨부된 적 없는 파일은 발표 화면에 지정할 수 없다")
    void updatePresentationContent_RejectsImageNotAttachedToOurSubmission() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        // 다른 팀이 올렸든, 아무 데도 첨부된 적 없는 파일이든 — 우리 제출물 버전 이력에 없으면 동일하게 거부돼야 한다.
        FileObject unattachedFile = fileObjectRepository.save(
                FileObject.create(NON_MEMBER, "key", "screen.png", "image/png", 1024L, true, "IMAGE"));
        JsonNode screens = new ObjectMapper().readTree(
                "[{\"imageFileId\": " + unattachedFile.getId() + ", \"caption\": \"홈 화면\"}]");

        assertThatThrownBy(() -> submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, screens, null)))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionPresentationImageOwnershipException.class);
    }

    @Test
    @DisplayName("우리 제출물에 FILE 아티팩트로 첨부된 적 있는 이미지는 발표 화면에 지정할 수 있다")
    void updatePresentationContent_AllowsImageAttachedToOurSubmission() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        FileObject ourFile = fileObjectRepository.save(
                FileObject.create(MEMBER, "key", "screen.png", "image/png", 1024L, true, "IMAGE"));
        SubmissionVersion version = submissionVersionRepository.save(
                SubmissionVersion.create(submission.getId(), 1, "1차 제출", null, MEMBER, false));
        submissionArtifactRepository.saveAll(
                List.of(SubmissionArtifact.file(version.getId(), null, ourFile.getId())));
        JsonNode screens = new ObjectMapper().readTree(
                "[{\"imageFileId\": " + ourFile.getId() + ", \"caption\": \"홈 화면\"}]");

        PresentationContentResponse response = submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, screens, null));

        assertThat(response.introText()).isEqualTo("소개");
        // screens는 imageFileId뿐 아니라 다운로드 가능한 imageUrl로도 보강돼야 한다 — 다른 팀
        // 사용자가 공개 발표자료를 볼 때 imageFileId만으로는 이미지를 못 띄우던 문제
        // (sunzx0428 PR #87 리뷰 09-03).
        assertThat(response.screens().get(0).get("imageUrl").asText()).isEqualTo("https://fake-storage.local/key");
    }

    @Test
    @DisplayName("다른 팀 사용자가 조회하는 공개 발표자료에도 imageUrl이 보강된다")
    void getPresentationContent_ResolvesImageUrlForOtherTeamViewer() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        FileObject ourFile = fileObjectRepository.save(
                FileObject.create(MEMBER, "screen-key", "screen.png", "image/png", 1024L, true, "IMAGE"));
        SubmissionVersion version = submissionVersionRepository.save(
                SubmissionVersion.create(submission.getId(), 1, "1차 제출", null, MEMBER, false));
        submissionArtifactRepository.saveAll(
                List.of(SubmissionArtifact.file(version.getId(), null, ourFile.getId())));
        JsonNode screens = new ObjectMapper().readTree(
                "[{\"imageFileId\": " + ourFile.getId() + ", \"caption\": \"홈 화면\"}]");
        submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, screens, null));

        // when: 팀 소속이 아닌 사용자가 공개 발표자료를 조회
        PresentationContentResponse fetched = submissionFacade.getPresentationContent(submission.getId(), NON_MEMBER);

        // then
        assertThat(fetched.screens().get(0).get("imageUrl").asText()).isEqualTo("https://fake-storage.local/screen-key");
    }

    @Test
    @DisplayName("screens가 배열이 아니면 거부된다")
    void updatePresentationContent_RejectsNonArrayScreens() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        JsonNode screens = new ObjectMapper().readTree("{\"imageFileId\": 1}");

        assertThatThrownBy(() -> submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, screens, null)))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionInvalidScreensException.class);
    }

    @Test
    @DisplayName("screens 원소의 imageFileId가 숫자가 아니면 거부된다")
    void updatePresentationContent_RejectsNonNumericImageFileId() throws Exception {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(presentationMilestone()));
        JsonNode screens = new ObjectMapper().readTree("[{\"imageFileId\": \"not-a-number\"}]");

        assertThatThrownBy(() -> submissionFacade.updatePresentationContent(
                submission.getId(), MEMBER, new PresentationContentRequest("소개", null, screens, null)))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionInvalidScreensException.class);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 팀원 행이 남아있어도 우리팀 제출 조회·생성을 할 수 없다")
    void getMyTeamSubmission_RejectsWithdrawnEnrollment() {
        teamMemberRepository.save(TeamMember.create(TEAM_ID, "202677775", false, "탈퇴예정"));
        teamMemberRepository.assignTeamToSection(TEAM_ID, SECTION_ID);
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.getMyTeamSubmission(MILESTONE_ID, "202677775"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("아무도 확인하지 않았으면 확인 인원 0명, 본인 확인 여부는 false로 조회된다")
    void getMemberConsent_ReportsZeroBeforeAnyoneConfirms() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionMemberConsentResponse response = submissionFacade.getMemberConsent(submission.getId(), MEMBER);

        assertThat(response.confirmedCount()).isZero();
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.isConfirmedByMe()).isFalse();
    }

    @Test
    @DisplayName("확인을 등록하면 확인 인원과 본인 확인 여부가 바로 반영된다")
    void confirmAsMember_ReflectsInConsentImmediately() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        SubmissionMemberConsentResponse response = submissionFacade.confirmAsMember(submission.getId(), MEMBER);

        assertThat(response.confirmedCount()).isEqualTo(1);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.isConfirmedByMe()).isTrue();
    }

    @Test
    @DisplayName("확인을 취소하면 확인 인원과 본인 확인 여부가 다시 줄어든다")
    void cancelConfirmation_ReflectsInConsentImmediately() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        submissionFacade.confirmAsMember(submission.getId(), MEMBER);

        SubmissionMemberConsentResponse response = submissionFacade.cancelConfirmation(submission.getId(), MEMBER);

        assertThat(response.confirmedCount()).isZero();
        assertThat(response.isConfirmedByMe()).isFalse();
    }

    private Milestone milestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
    }

    private Milestone presentationMilestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "발표", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
                MilestoneType.PRESENTATION);
    }
}
