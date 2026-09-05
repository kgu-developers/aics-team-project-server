package submission.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.admin.submission.application.SubmissionAdminFacade;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminListResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminDetailResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminListResponse;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.submission.application.query.SubmissionQueryService;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionStatus;
import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.exception.SubmissionVersionNotFoundException;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;

import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakeSubmissionArtifactRepository;
import mock.repository.FakeSubmissionRepository;
import mock.repository.FakeSubmissionVersionRepository;
import mock.repository.FakeTeamRepository;

class SubmissionAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final Long OTHER_TEAM_ID = 20L;
    private static final Long MILESTONE_ID = 5L;
    private static final String PROFESSOR = "professor1";
    private static final String OTHER_PROFESSOR = "professor2";

    private MilestoneRepository milestoneRepository;
    private SectionQueryService sectionQueryService;
    private FakeTeamRepository teamRepository;
    private FakeSubmissionRepository submissionRepository;
    private FakeSubmissionVersionRepository submissionVersionRepository;
    private SubmissionAdminFacade submissionAdminFacade;
    private Long teamId;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        sectionQueryService = mock(SectionQueryService.class);

        teamRepository = new FakeTeamRepository();
        Team team = teamRepository.save(Team.builder()
                .sectionId(SECTION_ID)
                .name("A팀")
                .status(Status.CONFIRMED)
                .build());
        teamId = team.getId();

        submissionRepository = new FakeSubmissionRepository();
        submissionVersionRepository = new FakeSubmissionVersionRepository();
        FakeSubmissionArtifactRepository submissionArtifactRepository = new FakeSubmissionArtifactRepository();
        FakeFileObjectRepository fileObjectRepository = new FakeFileObjectRepository();
        FakeFileStorage fileStorage = new FakeFileStorage();

        SubmissionQueryService submissionQueryService = new SubmissionQueryService(
                submissionRepository, milestoneRepository,
                mock(org.springframework.transaction.PlatformTransactionManager.class));

        submissionAdminFacade = new SubmissionAdminFacade(
                milestoneRepository,
                sectionQueryService,
                teamRepository,
                submissionQueryService,
                submissionVersionRepository,
                submissionArtifactRepository,
                fileObjectRepository,
                fileStorage
        );
    }

    @Test
    @DisplayName("담당 교수는 마일스톤의 팀별 제출 현황을 조회할 수 있다")
    void getSubmissionsByMilestone_AllowsOwningProfessor() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionAdminListResponse response = submissionAdminFacade.getSubmissionsByMilestone(MILESTONE_ID, PROFESSOR);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).teamId()).isEqualTo(teamId);
        assertThat(response.contents().get(0).status()).isEqualTo(SubmissionStatus.NOT_SUBMITTED);
    }

    @Test
    @DisplayName("아직 한 번도 조회되지 않은 팀도 not_submitted 상태로 함께 조회된다")
    void getSubmissionsByMilestone_IncludesTeamsWithoutSubmissionRow() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionAdminListResponse response = submissionAdminFacade.getSubmissionsByMilestone(MILESTONE_ID, PROFESSOR);

        assertThat(submissionRepository.findByTeamIdAndMilestoneId(teamId, MILESTONE_ID)).isPresent();
        assertThat(response.contents()).extracting(SubmissionAdminResponse::teamId).containsExactly(teamId);
    }

    @Test
    @DisplayName("담당 교수가 아니면 팀별 제출 현황을 조회할 수 없다")
    void getSubmissionsByMilestone_RejectsNonOwningProfessor() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, OTHER_PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> submissionAdminFacade.getSubmissionsByMilestone(MILESTONE_ID, OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("담당 교수는 제출 상세를 조회할 수 있다")
    void getSubmission_AllowsOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionAdminResponse response = submissionAdminFacade.getSubmission(submission.getId(), PROFESSOR);

        assertThat(response.id()).isEqualTo(submission.getId());
        assertThat(response.teamName()).isEqualTo("A팀");
    }

    @Test
    @DisplayName("다른 분반 담당 교수는 제출 상세를 조회할 수 없다")
    void getSubmission_RejectsNonOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, OTHER_PROFESSOR)).willReturn(false);

        assertThatThrownBy(() -> submissionAdminFacade.getSubmission(submission.getId(), OTHER_PROFESSOR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("담당 교수는 버전 이력을 조회할 수 있다")
    void getVersions_AllowsOwningProfessor() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionVersionAdminListResponse response = submissionAdminFacade.getVersions(submission.getId(), PROFESSOR);

        assertThat(response.contents()).hasSize(1);
    }

    @Test
    @DisplayName("담당 교수는 버전 상세와 아티팩트를 함께 조회할 수 있다")
    void getVersion_AllowsOwningProfessorAndIncludesArtifacts() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        SubmissionVersion version = submissionVersionRepository.save(SubmissionVersion.create(
                submission.getId(), 1, "설명", "변경사항", "202412345", false));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        SubmissionVersionAdminDetailResponse response = submissionAdminFacade.getVersion(submission.getId(), version.getVersion(), PROFESSOR);

        assertThat(response.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 버전을 조회하면 예외를 던진다")
    void getVersion_NotFound_ThrowsException() {
        Submission submission = submissionRepository.save(Submission.create(teamId, MILESTONE_ID));
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR)).willReturn(true);

        assertThatThrownBy(() -> submissionAdminFacade.getVersion(submission.getId(), 99, PROFESSOR))
                .isInstanceOf(SubmissionVersionNotFoundException.class);
    }

    private Milestone milestone() {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "마일스톤", null, 2, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null));
    }
}
