package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kgu.developers.admin.evaluation.application.PeerEvaluationAdminFacade;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamDetailResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamSummaryResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswerRepository;
import kgu.developers.domain.evaluation.exception.PeerEvaluationFormNotFoundException;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
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
class PeerEvaluationAdminFacadeTest {

    @Mock
    private PeerEvaluationFormRepository formRepository;

    @Mock
    private PeerEvaluationSubmissionRepository submissionRepository;

    @Mock
    private PeerEvaluationTeammateAnswerRepository teammateAnswerRepository;

    @Mock
    private SectionQueryService sectionQueryService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private MeetingRecordQueryService meetingRecordQueryService;

    @InjectMocks
    private PeerEvaluationAdminFacade facade;

    private static final String PROFESSOR_ID = "202012345";
    private static final Long SECTION_ID = 1L;

    @Test
    @DisplayName("분반 상호평가 목록 조회가 정상 동작한다")
    void getPeerEvaluations_Success() {
        // given
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);

        Team team1 = Team.builder().id(10L).sectionId(SECTION_ID).name("OOP-01 - 1팀").build();
        Team team2 = Team.builder().id(20L).sectionId(SECTION_ID).name("OOP-01 - 2팀").build();
        given(teamRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(team1, team2));

        Enrollment en1 = Enrollment.builder().sectionId(SECTION_ID).userId("20260001").role(kgu.developers.domain.enrollment.domain.Role.STUDENT).status(kgu.developers.domain.enrollment.domain.Status.ACTIVE).build();
        Enrollment en2 = Enrollment.builder().sectionId(SECTION_ID).userId("20260002").role(kgu.developers.domain.enrollment.domain.Role.STUDENT).status(kgu.developers.domain.enrollment.domain.Status.ACTIVE).build();
        Enrollment en3 = Enrollment.builder().sectionId(SECTION_ID).userId("20260003").role(kgu.developers.domain.enrollment.domain.Role.STUDENT).status(kgu.developers.domain.enrollment.domain.Status.ACTIVE).build();
        Enrollment en4 = Enrollment.builder().sectionId(SECTION_ID).userId("20260004").role(kgu.developers.domain.enrollment.domain.Role.STUDENT).status(kgu.developers.domain.enrollment.domain.Status.WITHDRAWN).build(); // inactive
        given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(en1, en2, en3, en4));

        TeamMember m1 = TeamMember.builder().id(1L).teamId(10L).userId("20260001").isLeader(true).build();
        TeamMember m2 = TeamMember.builder().id(2L).teamId(10L).userId("20260002").isLeader(false).build();
        TeamMember m3 = TeamMember.builder().id(3L).teamId(20L).userId("20260003").isLeader(true).build();
        TeamMember m4 = TeamMember.builder().id(4L).teamId(20L).userId("20260004").isLeader(false).build();
        given(teamMemberRepository.findAllByTeamIdIn(List.of(10L, 20L))).willReturn(List.of(m1, m2, m3, m4));

        given(meetingRecordQueryService.countMeetingRecords(List.of(10L, 20L)))
            .willReturn(Map.of(10L, 3L, 20L, 1L));

        PeerEvaluationForm form = PeerEvaluationForm.restore(
            100L, SECTION_ID, 5L, false,
            LocalDateTime.of(2026, 12, 1, 0, 0),
            LocalDateTime.of(2026, 12, 15, 23, 59),
            null, null, null
        );
        given(formRepository.findAllBySectionIdOrderByIdDesc(SECTION_ID)).willReturn(List.of(form));

        LocalDateTime submittedAt = LocalDateTime.of(2026, 12, 14, 15, 30);
        PeerEvaluationSubmission sub1 = PeerEvaluationSubmission.builder()
            .id(1001L)
            .formId(100L)
            .evaluatorId("20260001")
            .status(PeerEvaluationSubmissionStatus.SUBMITTED)
            .submittedAt(submittedAt)
            .build();
        PeerEvaluationSubmission sub2 = PeerEvaluationSubmission.builder()
            .id(1002L)
            .formId(100L)
            .evaluatorId("20260002")
            .status(PeerEvaluationSubmissionStatus.DRAFT)
            .build();
        given(submissionRepository.findAllByFormId(100L)).willReturn(List.of(sub1, sub2));

        // when
        PeerEvaluationAdminListResponse response = facade.getPeerEvaluations(SECTION_ID, null, PROFESSOR_ID);

        // then
        assertThat(response.sectionId()).isEqualTo(SECTION_ID);
        assertThat(response.formId()).isEqualTo(100L);
        assertThat(response.teams()).hasSize(2);

        PeerEvaluationAdminTeamSummaryResponse team1Summary = response.teams().get(0);
        assertThat(team1Summary.teamId()).isEqualTo(10L);
        assertThat(team1Summary.teamName()).isEqualTo("OOP-01 - 1팀");
        assertThat(team1Summary.submittedCount()).isEqualTo(1);
        assertThat(team1Summary.totalMemberCount()).isEqualTo(2);
        assertThat(team1Summary.lastSubmittedAt()).isEqualTo(submittedAt);
        assertThat(team1Summary.meetingRecordCount()).isEqualTo(3L);

        PeerEvaluationAdminTeamSummaryResponse team2Summary = response.teams().get(1);
        assertThat(team2Summary.teamId()).isEqualTo(20L);
        assertThat(team2Summary.submittedCount()).isEqualTo(0);
        assertThat(team2Summary.totalMemberCount()).isEqualTo(1); // 20260004 is inactive
        assertThat(team2Summary.lastSubmittedAt()).isNull();
        assertThat(team2Summary.meetingRecordCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("담당 교수가 아닌 경우 접근이 거부된다")
    void getPeerEvaluations_AccessDenied() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> facade.getPeerEvaluations(SECTION_ID, null, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("양식이 생성되지 않은 분반도 빈 목록/0건으로 정상 응답한다")
    void getPeerEvaluations_NoForm() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);

        Team team1 = Team.builder().id(10L).sectionId(SECTION_ID).name("OOP-01 - 1팀").build();
        given(teamRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(team1));
        given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of());
        given(teamMemberRepository.findAllByTeamIdIn(List.of(10L))).willReturn(List.of());
        given(meetingRecordQueryService.countMeetingRecords(List.of(10L))).willReturn(Map.of());
        given(formRepository.findAllBySectionIdOrderByIdDesc(SECTION_ID)).willReturn(List.of());

        PeerEvaluationAdminListResponse response = facade.getPeerEvaluations(SECTION_ID, null, PROFESSOR_ID);

        assertThat(response.formId()).isNull();
        assertThat(response.opensAt()).isNull();
        assertThat(response.closesAt()).isNull();
        assertThat(response.teams()).hasSize(1);
        assertThat(response.teams().get(0).submittedCount()).isZero();
    }

    @Test
    @DisplayName("팀 상호평가 상세 조회가 정상 동작한다")
    void getTeamPeerEvaluationDetail_Success() {
        // given
        Long teamId = 10L;
        Long formId = 100L;
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);

        Team team = Team.builder().id(teamId).sectionId(SECTION_ID).name("OOP-01 - 1팀").build();
        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));

        PeerEvaluationForm form = PeerEvaluationForm.restore(
            formId, SECTION_ID, 5L, false,
            LocalDateTime.of(2026, 12, 1, 0, 0),
            LocalDateTime.of(2026, 12, 15, 23, 59),
            null, null, null
        );
        given(formRepository.findAllBySectionIdOrderByIdDesc(SECTION_ID)).willReturn(List.of(form));

        Enrollment en1 = Enrollment.builder().sectionId(SECTION_ID).userId("20260001").role(kgu.developers.domain.enrollment.domain.Role.STUDENT).status(kgu.developers.domain.enrollment.domain.Status.ACTIVE).build();
        Enrollment en2 = Enrollment.builder().sectionId(SECTION_ID).userId("20260002").role(kgu.developers.domain.enrollment.domain.Role.STUDENT).status(kgu.developers.domain.enrollment.domain.Status.ACTIVE).build();
        given(enrollmentRepository.findAllBySectionId(SECTION_ID)).willReturn(List.of(en1, en2));

        TeamMember leader = TeamMember.builder().id(1L).teamId(teamId).userId("20260001").isLeader(true).projectRole("백엔드").build();
        TeamMember member = TeamMember.builder().id(2L).teamId(teamId).userId("20260002").isLeader(false).projectRole("프론트").build();
        given(teamMemberRepository.findAllByTeamId(teamId)).willReturn(List.of(leader, member));

        User user1 = User.builder().studentNumber("20260001").name("김민준").build();
        User user2 = User.builder().studentNumber("20260002").name("이서연").build();
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of("20260001", "20260002")))
            .willReturn(List.of(user1, user2));

        LocalDateTime submittedAt = LocalDateTime.of(2026, 12, 14, 15, 30);
        PeerEvaluationSubmission sub1 = PeerEvaluationSubmission.builder()
            .id(1001L)
            .formId(formId)
            .evaluatorId("20260001")
            .status(PeerEvaluationSubmissionStatus.SUBMITTED)
            .submittedAt(submittedAt)
            .selfContribution("백엔드 설계")
            .projectReviewComment("협업 우수")
            .reflectionComment("좋은 경험")
            .build();
        given(submissionRepository.findAllByFormIdAndEvaluatorIdIn(formId, List.of("20260001", "20260002")))
            .willReturn(List.of(sub1));

        PeerEvaluationTeammateAnswer answer = PeerEvaluationTeammateAnswer.create(
            1001L, "20260002", 100, "UI 개발", "성실한 참여"
        );
        given(teammateAnswerRepository.findAllBySubmissionIdIn(List.of(1001L))).willReturn(List.of(answer));

        MeetingRecord meetingRecord = MeetingRecord.create(
            teamId, "프로젝트 킥오프", MeetingPhase.MID_CHECK, "20260001",
            LocalDateTime.of(2026, 10, 1, 14, 0), "회의실", "내용", List.of("20260001", "20260002")
        );
        given(meetingRecordQueryService.getMeetingRecords(teamId, null)).willReturn(List.of(meetingRecord));

        // when
        PeerEvaluationAdminTeamDetailResponse detail = facade.getTeamPeerEvaluationDetail(SECTION_ID, teamId, null, PROFESSOR_ID);

        // then
        assertThat(detail.teamId()).isEqualTo(teamId);
        assertThat(detail.teamName()).isEqualTo("OOP-01 - 1팀");
        assertThat(detail.formId()).isEqualTo(formId);
        assertThat(detail.members()).hasSize(2);

        // Members verification
        var leaderMember = detail.members().get(0);
        assertThat(leaderMember.userId()).isEqualTo("20260001");
        assertThat(leaderMember.name()).isEqualTo("김민준");
        assertThat(leaderMember.isLeader()).isTrue();
        assertThat(leaderMember.role()).isEqualTo("팀장 · 백엔드");
        assertThat(leaderMember.averageReceivedScore()).isNull(); // member hasn't submitted yet

        var regularMember = detail.members().get(1);
        assertThat(regularMember.userId()).isEqualTo("20260002");
        assertThat(regularMember.name()).isEqualTo("이서연");
        assertThat(regularMember.isLeader()).isFalse();
        assertThat(regularMember.role()).isEqualTo("프론트");
        assertThat(regularMember.averageReceivedScore()).isEqualTo(100.0);

        // Evaluations verification
        assertThat(detail.evaluations()).hasSize(2);
        var eval1 = detail.evaluations().get(0);
        assertThat(eval1.evaluatorId()).isEqualTo("20260001");
        assertThat(eval1.evaluatorName()).isEqualTo("김민준");
        assertThat(eval1.status()).isEqualTo(PeerEvaluationSubmissionStatus.SUBMITTED);
        assertThat(eval1.averageScore()).isEqualTo(100.0);
        assertThat(eval1.selfContribution()).isEqualTo("백엔드 설계");
        assertThat(eval1.scores()).hasSize(2);
        assertThat(eval1.scores().get(0).isSelf()).isTrue();
        assertThat(eval1.scores().get(0).contributionPercent()).isNull();
        assertThat(eval1.scores().get(1).isSelf()).isFalse();
        assertThat(eval1.scores().get(1).contributionPercent()).isEqualTo(100);
        assertThat(eval1.teammateAssessments()).hasSize(1);
        assertThat(eval1.teammateAssessments().get(0).targetUserId()).isEqualTo("20260002");
        assertThat(eval1.teammateAssessments().get(0).contributionDetail()).isEqualTo("UI 개발");

        var eval2 = detail.evaluations().get(1);
        assertThat(eval2.evaluatorId()).isEqualTo("20260002");
        assertThat(eval2.status()).isNull();
        assertThat(eval2.averageScore()).isNull();
        assertThat(eval2.scores().get(0).contributionPercent()).isNull(); // not submitted

        // Meeting records verification
        assertThat(detail.meetingRecords()).hasSize(1);
        assertThat(detail.meetingRecords().get(0).title()).isEqualTo("프로젝트 킥오프");
    }

    @Test
    @DisplayName("다른 분반의 팀을 조회하려고 하면 접근이 거부된다")
    void getTeamPeerEvaluationDetail_OtherSectionTeam_AccessDenied() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        Team team = Team.builder().id(10L).sectionId(999L).name("다른분반팀").build();
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));

        assertThatThrownBy(() -> facade.getTeamPeerEvaluationDetail(SECTION_ID, 10L, null, PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("팀이 존재하지 않으면 예외가 발생한다")
    void getTeamPeerEvaluationDetail_TeamNotFound() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> facade.getTeamPeerEvaluationDetail(SECTION_ID, 10L, null, PROFESSOR_ID))
            .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    @DisplayName("상호평가 양식이 없으면 예외가 발생한다")
    void getTeamPeerEvaluationDetail_FormNotFound() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        Team team = Team.builder().id(10L).sectionId(SECTION_ID).name("1팀").build();
        given(teamRepository.findById(10L)).willReturn(Optional.of(team));
        given(formRepository.findAllBySectionIdOrderByIdDesc(SECTION_ID)).willReturn(List.of());

        assertThatThrownBy(() -> facade.getTeamPeerEvaluationDetail(SECTION_ID, 10L, null, PROFESSOR_ID))
            .isInstanceOf(PeerEvaluationFormNotFoundException.class);
    }
}
