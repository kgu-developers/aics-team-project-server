package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.api.evaluation.application.TeamEvaluationFacade;
import kgu.developers.api.evaluation.presentation.TeamEvaluationWindowState;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationScoreRequest;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationSubmitRequest;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScoreRepository;
import kgu.developers.domain.evaluation.exception.InvalidTeamEvaluationResponseException;
import kgu.developers.domain.evaluation.exception.TeamEvaluationClosedException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TeamEvaluationFacadeTest {
    private static final Long MILESTONE_ID = 1L;
    private static final Long SECTION_ID = 2L;
    private static final Long MY_TEAM_ID = 3L;
    private static final Long TARGET_TEAM_ID = 4L;
    private static final String USER_ID = "20260001";

    @Mock private MilestoneRepository milestoneRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserQueryService userQueryService;
    @Mock private TeamEvaluationCriterionRepository criterionRepository;
    @Mock private TeamEvaluationRepository evaluationRepository;
    @Mock private TeamEvaluationScoreRepository scoreRepository;
    @Mock private SubmissionRepository submissionRepository;
    @InjectMocks private TeamEvaluationFacade facade;

    @BeforeEach
    void setUp() {
        given(userQueryService.getUserByStudentNumber(USER_ID)).willReturn(user());
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(openMilestone()));
    }

    @Test
    @DisplayName("학생은 평가 기간과 항목, 본인이 제출한 팀별 발표 평가를 조회한다")
    void getMyEvaluations() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(enrollment()));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(membership()));
        TeamEvaluationCriterion criterion = criterion(10L, "발표 완성도", 10, 1);
        TeamEvaluation evaluation = TeamEvaluation.restore(
                20L, MILESTONE_ID, USER_ID, TARGET_TEAM_ID,
                LocalDateTime.now(), null, null, null
        );
        given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(SECTION_ID))
                .willReturn(List.of(criterion));
        given(evaluationRepository.findAllByMilestoneIdAndRaterId(MILESTONE_ID, USER_ID))
                .willReturn(List.of(evaluation));
        given(scoreRepository.findAllByTeamEvaluationIds(List.of(20L)))
                .willReturn(List.of(TeamEvaluationScore.restore(30L, 20L, 10L, 8, null, null, null)));

        var response = facade.getMyEvaluations(MILESTONE_ID, USER_ID);

        assertThat(response.windowState()).isEqualTo(TeamEvaluationWindowState.OPEN);
        assertThat(response.criteria()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(10L);
            assertThat(item.maxScore()).isEqualTo(10);
        });
        assertThat(response.evaluations()).singleElement().satisfies(item -> {
            assertThat(item.teamId()).isEqualTo(TARGET_TEAM_ID);
            assertThat(item.scores()).singleElement().satisfies(score ->
                    assertThat(score.score()).isEqualTo(8));
        });
        then(scoreRepository).should().findAllByTeamEvaluationIds(List.of(20L));
        then(scoreRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("학생은 다른 팀의 모든 활성 평가 항목 점수를 제출한다")
    void submitEvaluation() {
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, USER_ID))
                .willReturn(Optional.of(enrollment()));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(membership()));
        given(teamRepository.findById(TARGET_TEAM_ID)).willReturn(Optional.of(targetTeam()));
        given(submissionRepository.findByTeamIdAndMilestoneId(TARGET_TEAM_ID, MILESTONE_ID))
                .willReturn(Optional.of(Submission.create(TARGET_TEAM_ID, MILESTONE_ID)));
        given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(SECTION_ID)).willReturn(List.of(
                criterion(10L, "발표 완성도", 10, 1),
                criterion(11L, "질의응답", 5, 2)
        ));
        given(evaluationRepository.findByMilestoneIdAndRaterIdAndRateeTeamId(
                MILESTONE_ID, USER_ID, TARGET_TEAM_ID)).willReturn(Optional.empty());
        given(evaluationRepository.save(any())).willAnswer(invocation -> {
            TeamEvaluation value = invocation.getArgument(0);
            return value.getId() == null
                    ? TeamEvaluation.restore(20L, value.getMilestoneId(), value.getRaterId(),
                            value.getRateeTeamId(), value.getSubmittedAt(), null, null, null)
                    : value;
        });
        given(scoreRepository.saveAll(any())).willAnswer(invocation -> {
            List<TeamEvaluationScore> values = invocation.getArgument(0);
            return List.of(
                    TeamEvaluationScore.restore(30L, 20L, values.get(0).getCriterionId(),
                            values.get(0).getScore(), null, null, null),
                    TeamEvaluationScore.restore(31L, 20L, values.get(1).getCriterionId(),
                            values.get(1).getScore(), null, null, null)
            );
        });

        var response = facade.submit(MILESTONE_ID, TARGET_TEAM_ID, USER_ID,
                new TeamEvaluationSubmitRequest(List.of(
                        new TeamEvaluationScoreRequest(10L, 9),
                        new TeamEvaluationScoreRequest(11L, 4)
                )));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.submittedAt()).isNotNull();
        assertThat(response.scores()).extracting("score").containsExactly(9, 4);
        then(scoreRepository).should().deleteAllByTeamEvaluationId(20L);
        then(scoreRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("발표 마일스톤의 제출 대상이 아닌 팀은 평가할 수 없다")
    void rejectsTeamWithoutPresentationSubmission() {
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, USER_ID))
                .willReturn(Optional.of(enrollment()));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(membership()));
        given(teamRepository.findById(TARGET_TEAM_ID)).willReturn(Optional.of(targetTeam()));
        given(submissionRepository.findByTeamIdAndMilestoneId(TARGET_TEAM_ID, MILESTONE_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.submit(MILESTONE_ID, TARGET_TEAM_ID, USER_ID,
                new TeamEvaluationSubmitRequest(List.of(new TeamEvaluationScoreRequest(10L, 5)))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 발표 마일스톤의 제출 대상 팀만 평가할 수 있습니다.");
    }

    @Test
    @DisplayName("본인 팀은 발표 평가할 수 없다")
    void rejectsOwnTeam() {
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, USER_ID))
                .willReturn(Optional.of(enrollment()));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(membership()));
        given(teamRepository.findById(MY_TEAM_ID)).willReturn(Optional.of(Team.builder()
                .id(MY_TEAM_ID).sectionId(SECTION_ID).name("내 팀").build()));

        assertThatThrownBy(() -> facade.submit(MILESTONE_ID, MY_TEAM_ID, USER_ID,
                new TeamEvaluationSubmitRequest(List.of(new TeamEvaluationScoreRequest(10L, 5)))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("활성 평가 항목이 누락되면 발표 평가 제출을 거부한다")
    void rejectsMissingCriterion() {
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, USER_ID))
                .willReturn(Optional.of(enrollment()));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(membership()));
        given(teamRepository.findById(TARGET_TEAM_ID)).willReturn(Optional.of(targetTeam()));
        given(submissionRepository.findByTeamIdAndMilestoneId(TARGET_TEAM_ID, MILESTONE_ID))
                .willReturn(Optional.of(Submission.create(TARGET_TEAM_ID, MILESTONE_ID)));
        given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(SECTION_ID)).willReturn(List.of(
                criterion(10L, "발표 완성도", 10, 1),
                criterion(11L, "질의응답", 5, 2)
        ));

        assertThatThrownBy(() -> facade.submit(MILESTONE_ID, TARGET_TEAM_ID, USER_ID,
                new TeamEvaluationSubmitRequest(List.of(new TeamEvaluationScoreRequest(10L, 5)))))
                .isInstanceOf(InvalidTeamEvaluationResponseException.class);
    }

    @Test
    @DisplayName("평가 기간이 종료되면 발표 평가를 제출할 수 없다")
    void rejectsClosedWindow() {
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(closedMilestone()));
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, USER_ID))
                .willReturn(Optional.of(enrollment()));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(membership()));

        assertThatThrownBy(() -> facade.submit(MILESTONE_ID, TARGET_TEAM_ID, USER_ID,
                new TeamEvaluationSubmitRequest(List.of(new TeamEvaluationScoreRequest(10L, 5)))))
                .isInstanceOf(TeamEvaluationClosedException.class);
    }

    private static User user() {
        return User.builder().studentNumber(USER_ID).name("학생 A")
                .globalRole(UserGlobalRole.USER).build();
    }

    private static Enrollment enrollment() {
        return Enrollment.builder().id(1L).sectionId(SECTION_ID).userId(USER_ID)
                .role(Role.STUDENT).status(Status.ACTIVE).build();
    }

    private static TeamMember membership() {
        return TeamMember.builder().id(1L).teamId(MY_TEAM_ID).userId(USER_ID).build();
    }

    private static Team targetTeam() {
        return Team.builder().id(TARGET_TEAM_ID).sectionId(SECTION_ID).name("대상 팀").build();
    }

    private static TeamEvaluationCriterion criterion(Long id, String title, int maxScore, int order) {
        return TeamEvaluationCriterion.restore(id, SECTION_ID, title, maxScore, order, null, null, null);
    }

    private static Milestone openMilestone() {
        return milestone(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
    }

    private static Milestone closedMilestone() {
        return milestone(LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
    }

    private static Milestone milestone(LocalDateTime evaluationOpensAt, LocalDateTime evaluationClosesAt) {
        return Milestone.restore(
                MILESTONE_ID, SECTION_ID, "발표", null, 10, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(
                        LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(3),
                        null, null, evaluationOpensAt, evaluationClosesAt
                ),
                MilestoneType.PRESENTATION
        );
    }
}
