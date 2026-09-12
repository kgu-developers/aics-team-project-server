package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.admin.evaluation.application.PresentationEvaluationAdminFacade;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamDetailResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamSummaryResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScoreRepository;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PresentationEvaluationAdminFacadeTest {

    @Mock
    private SectionQueryService sectionQueryService;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private TeamEvaluationCriterionQueryService criterionQueryService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TeamEvaluationRepository teamEvaluationRepository;

    @Mock
    private TeamEvaluationScoreRepository teamEvaluationScoreRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private MeetingRecordQueryService meetingRecordQueryService;

    @InjectMocks
    private PresentationEvaluationAdminFacade facade;

    private static final String PROFESSOR_ID = "202012345";
    private static final Long SECTION_ID = 1L;

    @Test
    @DisplayName("분반 발표 평가 현황 목록 조회가 정상 동작한다")
    void getPresentationEvaluations_Success() {
        // given
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);

        LocalDateTime dueAt = LocalDateTime.of(2026, 12, 10, 23, 59);
        LocalDateTime evalOpen = LocalDateTime.of(2026, 12, 11, 0, 0);
        LocalDateTime evalClose = LocalDateTime.of(2026, 12, 15, 23, 59);
        MilestoneSchedule schedule = new MilestoneSchedule(null, dueAt, null, null, evalOpen, evalClose);
        Milestone milestone = Milestone.restore(
            100L, SECTION_ID, "최종 발표", "발표 평가", 15,
            MilestoneStatus.DRAFT, schedule, MilestoneType.PRESENTATION
        );
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));

        TeamEvaluationCriterion c1 = TeamEvaluationCriterion.restore(1L, SECTION_ID, "발표 완성도", 50, 0, null, null, null);
        TeamEvaluationCriterion c2 = TeamEvaluationCriterion.restore(2L, SECTION_ID, "질의응답", 50, 1, null, null, null);
        given(criterionQueryService.getCriteria(SECTION_ID)).willReturn(List.of(c1, c2));

        Team team1 = Team.builder().id(10L).sectionId(SECTION_ID).name("1팀").build();
        Team team2 = Team.builder().id(20L).sectionId(SECTION_ID).name("2팀").build();
        given(teamRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(team1, team2));

        Project p1 = Project.builder().id(101L).teamId(10L).title("AI 플랫폼").build();
        Project p2 = Project.builder().id(102L).teamId(20L).title("블록체인 시스템").build();
        given(projectRepository.findAllByTeamIdIn(List.of(10L, 20L))).willReturn(List.of(p1, p2));

        LocalDateTime submittedAt = LocalDateTime.of(2026, 12, 12, 14, 0);
        TeamEvaluation eval1 = TeamEvaluation.restore(1001L, 100L, "20260003", 10L, submittedAt, null, null, null);
        TeamEvaluation eval2 = TeamEvaluation.restore(1002L, 100L, "20260004", 10L, submittedAt, null, null, null);
        given(teamEvaluationRepository.findAllByMilestoneId(100L)).willReturn(List.of(eval1, eval2));

        TeamEvaluationScore s1_c1 = TeamEvaluationScore.restore(1L, 1001L, 1L, 40, null, null, null);
        TeamEvaluationScore s1_c2 = TeamEvaluationScore.restore(2L, 1001L, 2L, 50, null, null, null);
        TeamEvaluationScore s2_c1 = TeamEvaluationScore.restore(3L, 1002L, 1L, 30, null, null, null);
        TeamEvaluationScore s2_c2 = TeamEvaluationScore.restore(4L, 1002L, 2L, 40, null, null, null);
        given(teamEvaluationScoreRepository.findAllByTeamEvaluationIds(List.of(1001L, 1002L)))
            .willReturn(List.of(s1_c1, s1_c2, s2_c1, s2_c2));

        // when
        PresentationEvaluationAdminListResponse response =
            facade.getPresentationEvaluations(SECTION_ID, null, PROFESSOR_ID);

        // then
        assertThat(response.sectionId()).isEqualTo(SECTION_ID);
        assertThat(response.milestoneId()).isEqualTo(100L);
        assertThat(response.milestoneTitle()).isEqualTo("최종 발표");
        assertThat(response.closesAt()).isEqualTo(evalClose);
        assertThat(response.criteria()).hasSize(2);
        assertThat(response.criteria().get(0).title()).isEqualTo("발표 완성도");
        assertThat(response.teams()).hasSize(2);

        PresentationEvaluationAdminTeamSummaryResponse t1 = response.teams().get(0);
        assertThat(t1.teamId()).isEqualTo(10L);
        assertThat(t1.teamName()).isEqualTo("1팀");
        assertThat(t1.projectTitle()).isEqualTo("AI 플랫폼");
        assertThat(t1.evaluationCount()).isEqualTo(2);
        assertThat(t1.scores()).hasSize(2);
        assertThat(t1.scores().get(0).score()).isEqualTo(35.0); // (40 + 30) / 2
        assertThat(t1.scores().get(1).score()).isEqualTo(45.0); // (50 + 40) / 2
        assertThat(t1.totalScore()).isEqualTo(80.0); // 35.0 + 45.0

        PresentationEvaluationAdminTeamSummaryResponse t2 = response.teams().get(1);
        assertThat(t2.teamId()).isEqualTo(20L);
        assertThat(t2.teamName()).isEqualTo("2팀");
        assertThat(t2.projectTitle()).isEqualTo("블록체인 시스템");
        assertThat(t2.evaluationCount()).isZero();
        assertThat(t2.scores().get(0).score()).isNull();
        assertThat(t2.scores().get(1).score()).isNull();
        assertThat(t2.totalScore()).isNull();
    }

    @Test
    @DisplayName("명시적 milestoneId 지정 시 해당 마일스톤의 발표 평가 현황을 조회한다")
    void getPresentationEvaluations_WithMilestoneId_Success() {
        // given
        Long milestoneId = 100L;
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);

        LocalDateTime dueAt = LocalDateTime.of(2026, 12, 10, 23, 59);
        MilestoneSchedule schedule = new MilestoneSchedule(null, dueAt, null, null, null, null);
        Milestone milestone = Milestone.restore(
            milestoneId, SECTION_ID, "중간 발표", "발표", 8,
            MilestoneStatus.DRAFT, schedule, MilestoneType.PRESENTATION
        );
        given(milestoneRepository.findById(milestoneId)).willReturn(Optional.of(milestone));
        given(criterionQueryService.getCriteria(SECTION_ID)).willReturn(List.of());
        given(teamRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of());
        given(teamEvaluationRepository.findAllByMilestoneId(milestoneId)).willReturn(List.of());

        // when
        PresentationEvaluationAdminListResponse response =
            facade.getPresentationEvaluations(SECTION_ID, milestoneId, PROFESSOR_ID);

        // then
        assertThat(response.milestoneId()).isEqualTo(milestoneId);
        assertThat(response.milestoneTitle()).isEqualTo("중간 발표");
        assertThat(response.closesAt()).isEqualTo(dueAt); // evaluationClosesAt null -> dueAt
        assertThat(response.teams()).isEmpty();
    }

    @Test
    @DisplayName("발표 마일스톤이 존재하지 않으면 빈 팀 목록과 null 마일스톤 정보를 응답한다")
    void getPresentationEvaluations_NoMilestone_ReturnsEmpty() {
        // given
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of());
        given(criterionQueryService.getCriteria(SECTION_ID)).willReturn(List.of());

        // when
        PresentationEvaluationAdminListResponse response =
            facade.getPresentationEvaluations(SECTION_ID, null, PROFESSOR_ID);

        // then
        assertThat(response.sectionId()).isEqualTo(SECTION_ID);
        assertThat(response.milestoneId()).isNull();
        assertThat(response.milestoneTitle()).isNull();
        assertThat(response.closesAt()).isNull();
        assertThat(response.teams()).isEmpty();
    }

    @Test
    @DisplayName("명시적 milestoneId가 다른 분반이거나 발표 타입이 아니면 MilestoneNotFoundException이 발생한다")
    void getPresentationEvaluations_MilestoneMismatch_ThrowsException() {
        // given
        Long milestoneId = 999L;
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);

        MilestoneSchedule schedule = new MilestoneSchedule(null, LocalDateTime.of(2026, 12, 1, 0, 0), null, null, null, null);
        Milestone wrongTypeMilestone = Milestone.restore(
            milestoneId, SECTION_ID, "제안서", "제안서", 1,
            MilestoneStatus.DRAFT, schedule, MilestoneType.GENERAL
        );
        given(milestoneRepository.findById(milestoneId)).willReturn(Optional.of(wrongTypeMilestone));

        // when & then
        assertThatThrownBy(() -> facade.getPresentationEvaluations(SECTION_ID, milestoneId, PROFESSOR_ID))
            .isInstanceOf(MilestoneNotFoundException.class);
    }

    @Test
    @DisplayName("담당 분반이 아닌 경우 목록 조회 시 AccessDeniedException이 발생한다")
    void getPresentationEvaluations_AccessDenied() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> facade.getPresentationEvaluations(SECTION_ID, null, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀 발표 평가 결과 상세 조회가 정상 동작한다")
    void getTeamPresentationEvaluationDetail_Success() {
        // given
        Long teamId = 10L;
        Long milestoneId = 100L;
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);

        Team team1 = Team.builder().id(teamId).sectionId(SECTION_ID).name("1팀").build();
        Team team2 = Team.builder().id(20L).sectionId(SECTION_ID).name("2팀").build();
        given(teamRepository.findById(teamId)).willReturn(Optional.of(team1));

        LocalDateTime dueAt = LocalDateTime.of(2026, 12, 10, 23, 59);
        LocalDateTime evalOpen = LocalDateTime.of(2026, 12, 11, 0, 0);
        LocalDateTime evalClose = LocalDateTime.of(2026, 12, 15, 23, 59);
        MilestoneSchedule schedule = new MilestoneSchedule(null, dueAt, null, null, evalOpen, evalClose);
        Milestone milestone = Milestone.restore(
            milestoneId, SECTION_ID, "최종 발표", "발표", 15,
            MilestoneStatus.DRAFT, schedule, MilestoneType.PRESENTATION
        );
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of(milestone));

        TeamEvaluationCriterion c1 = TeamEvaluationCriterion.restore(1L, SECTION_ID, "완성도", 50, 0, null, null, null);
        TeamEvaluationCriterion c2 = TeamEvaluationCriterion.restore(2L, SECTION_ID, "발표력", 50, 1, null, null, null);
        given(criterionQueryService.getCriteria(SECTION_ID)).willReturn(List.of(c1, c2));

        Project project = Project.builder().id(101L).teamId(teamId).title("AI 플랫폼").build();
        given(projectRepository.findAllByTeamIdIn(List.of(teamId))).willReturn(List.of(project));

        Enrollment en1 = Enrollment.builder().sectionId(SECTION_ID).userId("20260001").role(Role.STUDENT).status(Status.ACTIVE).build(); // 1팀 소속 (피평가팀)
        Enrollment en2 = Enrollment.builder().sectionId(SECTION_ID).userId("20260002").role(Role.STUDENT).status(Status.ACTIVE).build(); // 2팀 소속 (평가자 후보)
        Enrollment en3 = Enrollment.builder().sectionId(SECTION_ID).userId("20260003").role(Role.STUDENT).status(Status.ACTIVE).build(); // 2팀 소속 (평가자 후보)
        Enrollment en4 = Enrollment.builder().sectionId(SECTION_ID).userId("20260004").role(Role.STUDENT).status(Status.WITHDRAWN).build(); // 비활성
        given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(en1, en2, en3, en4));

        given(teamRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(team1, team2));

        TeamMember tm1 = TeamMember.builder().id(1L).teamId(teamId).userId("20260001").isLeader(true).build();
        TeamMember tm2 = TeamMember.builder().id(2L).teamId(20L).userId("20260002").isLeader(true).build();
        TeamMember tm3 = TeamMember.builder().id(3L).teamId(20L).userId("20260003").isLeader(false).build();
        given(teamMemberRepository.findAllByTeamIdIn(List.of(teamId, 20L))).willReturn(List.of(tm1, tm2, tm3));

        User user2 = User.builder().studentNumber("20260002").name("이영희").build();
        User user3 = User.builder().studentNumber("20260003").name("박철수").build();
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of("20260002", "20260003")))
            .willReturn(List.of(user2, user3));

        LocalDateTime submittedAt = LocalDateTime.of(2026, 12, 12, 15, 0);
        TeamEvaluation evalSubmitted = TeamEvaluation.restore(1001L, milestoneId, "20260002", teamId, submittedAt, null, null, null);
        // 20260003 has not submitted
        given(teamEvaluationRepository.findAllByMilestoneIdAndRateeTeamId(milestoneId, teamId))
            .willReturn(List.of(evalSubmitted));

        TeamEvaluationScore score1 = TeamEvaluationScore.restore(1L, 1001L, 1L, 45, null, null, null);
        TeamEvaluationScore score2 = TeamEvaluationScore.restore(2L, 1001L, 2L, 40, null, null, null);
        given(teamEvaluationScoreRepository.findAllByTeamEvaluationIds(List.of(1001L)))
            .willReturn(List.of(score1, score2));

        MeetingRecord meetingRecord = MeetingRecord.builder()
            .id(501L)
            .teamId(teamId)
            .title("발표 리허설")
            .phase(MeetingPhase.FINAL)
            .authorId("20260001")
            .meetingAt(LocalDateTime.of(2026, 12, 8, 14, 0))
            .participants(List.of())
            .build();
        given(meetingRecordQueryService.getMeetingRecords(teamId, null)).willReturn(List.of(meetingRecord));

        // when
        PresentationEvaluationAdminTeamDetailResponse detail =
            facade.getTeamPresentationEvaluationDetail(SECTION_ID, teamId, null, PROFESSOR_ID);

        // then
        assertThat(detail.teamId()).isEqualTo(teamId);
        assertThat(detail.teamName()).isEqualTo("1팀");
        assertThat(detail.projectTitle()).isEqualTo("AI 플랫폼");
        assertThat(detail.milestoneId()).isEqualTo(milestoneId);
        assertThat(detail.closesAt()).isEqualTo(evalClose);
        assertThat(detail.criteria()).hasSize(2);

        // evaluations: 2 candidates (20260002 submitted, 20260003 not submitted)
        // sorted by isSubmitted desc, evaluatorId asc
        assertThat(detail.evaluations()).hasSize(2);

        var row1 = detail.evaluations().get(0);
        assertThat(row1.evaluatorId()).isEqualTo("20260002");
        assertThat(row1.evaluatorName()).isEqualTo("이영희");
        assertThat(row1.teamName()).isEqualTo("2팀");
        assertThat(row1.isSubmitted()).isTrue();
        assertThat(row1.submittedAt()).isEqualTo(submittedAt);
        assertThat(row1.scores()).hasSize(2);
        assertThat(row1.scores().get(0).score()).isEqualTo(45.0);
        assertThat(row1.scores().get(1).score()).isEqualTo(40.0);
        assertThat(row1.totalScore()).isEqualTo(85);

        var row2 = detail.evaluations().get(1);
        assertThat(row2.evaluatorId()).isEqualTo("20260003");
        assertThat(row2.evaluatorName()).isEqualTo("박철수");
        assertThat(row2.teamName()).isEqualTo("2팀");
        assertThat(row2.isSubmitted()).isFalse();
        assertThat(row2.submittedAt()).isNull();
        assertThat(row2.scores().get(0).score()).isNull();
        assertThat(row2.scores().get(1).score()).isNull();
        assertThat(row2.totalScore()).isNull();

        // meeting records
        assertThat(detail.meetingRecords()).hasSize(1);
        assertThat(detail.meetingRecords().get(0).id()).isEqualTo(501L);
        assertThat(detail.meetingRecords().get(0).title()).isEqualTo("발표 리허설");
    }

    @Test
    @DisplayName("다른 분반의 팀을 상세 조회하려고 하면 AccessDeniedException이 발생한다")
    void getTeamPresentationEvaluationDetail_OtherSectionTeam_ThrowsException() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        Team team = Team.builder().id(10L).sectionId(999L).name("타분반팀").build();
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));

        assertThatThrownBy(() -> facade.getTeamPresentationEvaluationDetail(SECTION_ID, 10L, null, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 팀을 상세 조회하면 TeamNotFoundException이 발생한다")
    void getTeamPresentationEvaluationDetail_TeamNotFound_ThrowsException() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getTeamPresentationEvaluationDetail(SECTION_ID, 10L, null, PROFESSOR_ID))
            .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    @DisplayName("상세 조회 시 발표 마일스톤이 없으면 MilestoneNotFoundException이 발생한다")
    void getTeamPresentationEvaluationDetail_NoMilestone_ThrowsException() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        Team team = Team.builder().id(10L).sectionId(SECTION_ID).name("1팀").build();
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of());

        assertThatThrownBy(() -> facade.getTeamPresentationEvaluationDetail(SECTION_ID, 10L, null, PROFESSOR_ID))
            .isInstanceOf(MilestoneNotFoundException.class);
    }

    @Test
    @DisplayName("담당 분반이 아닌 경우 상세 조회 시 AccessDeniedException이 발생한다")
    void getTeamPresentationEvaluationDetail_AccessDenied() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> facade.getTeamPresentationEvaluationDetail(SECTION_ID, 10L, null, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);
    }
}
