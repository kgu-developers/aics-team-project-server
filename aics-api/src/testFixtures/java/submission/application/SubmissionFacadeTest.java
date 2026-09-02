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

import kgu.developers.api.submission.application.SubmissionFacade;
import kgu.developers.api.submission.presentation.request.PresentationContentRequest;
import kgu.developers.api.submission.presentation.request.PresentationOrderRequest;
import kgu.developers.api.submission.presentation.request.SubmissionReopenRequest;
import kgu.developers.api.submission.presentation.response.MilestonePresentationsResponse;
import kgu.developers.api.submission.presentation.response.PresentationContentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.presentationcontent.application.command.PresentationContentCommandService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.command.SubmissionCommandService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionStatus;
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
        FakeSubmissionVersionRepository submissionVersionRepository = new FakeSubmissionVersionRepository();
        FakeSubmissionArtifactRepository submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        FakeSubmissionMemberConfirmationRepository submissionMemberConfirmationRepository =
                new FakeSubmissionMemberConfirmationRepository();
        FakePresentationContentRepository presentationContentRepository = new FakePresentationContentRepository();
        FakeFileObjectRepository fileObjectRepository = new FakeFileObjectRepository();
        FakeFileStorage fileStorage = new FakeFileStorage();
        FakeEnrollmentRepository enrollmentRepository = new FakeEnrollmentRepository();
        enrollmentRepository.save(Enrollment.create(SECTION_ID, LEADER, Role.STUDENT, Status.ACTIVE));
        enrollmentRepository.save(Enrollment.create(SECTION_ID, MEMBER, Role.STUDENT, Status.ACTIVE));
        FakeRequiredArtifactRepository requiredArtifactRepository = new FakeRequiredArtifactRepository();
        teamRepository = new FakeTeamRepository();
        editLockRepository = new FakeEditLockRepository();
        EditLockQueryService editLockQueryService = new EditLockQueryService(editLockRepository);

        SubmissionQueryService submissionQueryService =
                new SubmissionQueryService(submissionRepository, milestoneRepository);
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
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

        assertThatThrownBy(() -> submissionFacade.updatePresentationContent(
                submission.getId(), NON_MEMBER, new PresentationContentRequest("소개", null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀원은 발표 공개자료를 작성할 수 있고, 팀 소속이 아니어도 조회는 가능하다")
    void updateAndGetPresentationContent_TeamWriteAnyoneRead() {
        Submission submission = submissionRepository.save(Submission.create(TEAM_ID, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

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
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));

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
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
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
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
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
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionFacade.assignPresentationOrder(
                MILESTONE_ID, PROFESSOR, new PresentationOrderRequest(List.of(
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 1),
                        new PresentationOrderRequest.TeamOrder(TEAM_ID, 2)))))
                .isInstanceOf(kgu.developers.domain.submission.exception.SubmissionInvalidPresentationOrderException.class);
    }

    private Milestone milestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
    }
}
