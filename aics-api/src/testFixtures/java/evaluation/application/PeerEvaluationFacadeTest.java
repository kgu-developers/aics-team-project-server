package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.api.evaluation.application.PeerEvaluationFacade;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationAnswerRequest;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationResponseRequest;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionRepository;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswerRepository;
import kgu.developers.domain.evaluation.exception.InvalidPeerEvaluationResponseException;
import kgu.developers.domain.evaluation.exception.PeerEvaluationClosedException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
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
class PeerEvaluationFacadeTest {
    private static final Long FORM_ID = 1L;
    private static final Long SECTION_ID = 2L;
    private static final Long MILESTONE_ID = 3L;
    private static final Long TEAM_ID = 4L;
    private static final String REQUESTER = "20260001";
    private static final String ACTIVE_MEMBER = "20260002";
    private static final String ASSISTANT = "20260003";
    private static final String WITHDRAWN = "20260004";

    @Mock private PeerEvaluationFormRepository formRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserQueryService userQueryService;
    @Mock private PeerEvaluationSubmissionRepository submissionRepository;
    @Mock private PeerEvaluationTeammateAnswerRepository teammateAnswerRepository;
    @InjectMocks private PeerEvaluationFacade facade;

    @BeforeEach
    void setUpRequester() {
        given(userQueryService.getUserByStudentNumber(REQUESTER)).willReturn(user(REQUESTER, "학생 A", UserGlobalRole.USER));
        lenient().when(formRepository.findById(FORM_ID)).thenReturn(Optional.of(form()));
    }

    @Test
    @DisplayName("평가 컨텍스트는 공개된 발표 마일스톤과 최신 상호평가 양식 ID를 반환한다")
    void returnsEvaluationContext() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.STUDENT, Status.ACTIVE)));
        Milestone presentation = milestone(11L, MilestoneType.PRESENTATION, MilestoneStatus.PUBLISHED, 10);
        Milestone peerEvaluation = milestone(12L, MilestoneType.PEER_EVALUATION, MilestoneStatus.PUBLISHED, 11);
        Milestone draftPeerEvaluation = milestone(13L, MilestoneType.PEER_EVALUATION, MilestoneStatus.DRAFT, 12);
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID))
            .willReturn(List.of(presentation, peerEvaluation, draftPeerEvaluation));
        given(formRepository.findAllBySectionIdOrderByIdDesc(SECTION_ID)).willReturn(List.of(
            peerEvaluationForm(22L, 13L),
            peerEvaluationForm(21L, 12L)
        ));

        var response = facade.getContext(SECTION_ID, REQUESTER);

        assertThat(response.presentationMilestoneId()).isEqualTo("11");
        assertThat(response.peerEvaluationFormId()).isEqualTo("21");
    }

    @Test
    @DisplayName("평가가 아직 설정되지 않았으면 평가 컨텍스트 ID를 null로 반환한다")
    void returnsEmptyEvaluationContext() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.STUDENT, Status.ACTIVE)));
        given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(SECTION_ID)).willReturn(List.of());
        given(formRepository.findAllBySectionIdOrderByIdDesc(SECTION_ID)).willReturn(List.of());

        var response = facade.getContext(SECTION_ID, REQUESTER);

        assertThat(response.presentationMilestoneId()).isNull();
        assertThat(response.peerEvaluationFormId()).isNull();
    }

    @Test
    @DisplayName("활성 학생이 아니면 평가 컨텍스트를 조회할 수 없다")
    void rejectsEvaluationContextForAssistant() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.ASSISTANT, Status.ACTIVE)));

        assertThatThrownBy(() -> facade.getContext(SECTION_ID, REQUESTER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("본인과 조교, 탈퇴 학생을 제외한 같은 팀의 활성 학생만 평가 대상으로 반환한다")
    void returnsOnlyActiveStudentTeammates() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.STUDENT, Status.ACTIVE)));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(member(REQUESTER, true, "개발")));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(
            member(REQUESTER, true, "개발"),
            member(ACTIVE_MEMBER, false, "디자인"),
            member(ASSISTANT, false, "조교"),
            member(WITHDRAWN, false, "문서")
        ));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ACTIVE_MEMBER))
            .willReturn(Optional.of(enrollment(ACTIVE_MEMBER, Role.STUDENT, Status.ACTIVE)));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ASSISTANT))
            .willReturn(Optional.of(enrollment(ASSISTANT, Role.ASSISTANT, Status.ACTIVE)));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, WITHDRAWN))
            .willReturn(Optional.of(enrollment(WITHDRAWN, Role.STUDENT, Status.WITHDRAWN)));
        given(userQueryService.getUsersByStudentNumbers(List.of(ACTIVE_MEMBER)))
            .willReturn(List.of(user(ACTIVE_MEMBER, "학생 B", UserGlobalRole.USER)));
        given(milestoneRepository.findById(MILESTONE_ID)).willReturn(Optional.of(milestone()));
        given(submissionRepository.findByFormIdAndEvaluatorId(FORM_ID, REQUESTER)).willReturn(Optional.empty());

        var response = facade.getTargets(FORM_ID, REQUESTER);

        assertThat(response.formId()).isEqualTo(FORM_ID);
        assertThat(response.title()).isEqualTo("상호평가");
        assertThat(response.targets()).singleElement().satisfies(target -> {
            assertThat(target.userId()).isEqualTo(ACTIVE_MEMBER);
            assertThat(target.name()).isEqualTo("학생 B");
            assertThat(target.role()).isEqualTo("디자인");
        });
        assertThat(response.myResponse()).isNull();
    }

    @Test
    @DisplayName("활성 조교는 상호평가 대상 조회를 할 수 없다")
    void rejectsAssistantRequester() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.ASSISTANT, Status.ACTIVE)));

        assertThatThrownBy(() -> facade.getTargets(FORM_ID, REQUESTER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("활성 팀원 전체의 기여도 합계가 100이면 상호평가를 최종 제출한다")
    void submitsCompletePeerEvaluation() {
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.STUDENT, Status.ACTIVE)));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(member(REQUESTER, true, "개발")));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(
            member(REQUESTER, true, "개발"), member(ACTIVE_MEMBER, false, "디자인")
        ));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ACTIVE_MEMBER))
            .willReturn(Optional.of(enrollment(ACTIVE_MEMBER, Role.STUDENT, Status.ACTIVE)));
        given(submissionRepository.findByFormIdAndEvaluatorId(FORM_ID, REQUESTER)).willReturn(Optional.empty());
        given(submissionRepository.save(any())).willAnswer(invocation -> {
            var unsaved = invocation.<kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission>getArgument(0);
            return kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission.builder()
                .id(10L).formId(unsaved.getFormId()).evaluatorId(unsaved.getEvaluatorId())
                .selfContribution(unsaved.getSelfContribution()).projectReviewComment(unsaved.getProjectReviewComment())
                .reflectionComment(unsaved.getReflectionComment()).status(unsaved.getStatus())
                .submittedAt(unsaved.getSubmittedAt()).updatedAt(LocalDateTime.now()).build();
        });
        given(teammateAnswerRepository.saveAll(any())).willReturn(List.of(
            PeerEvaluationTeammateAnswer.create(10L, ACTIVE_MEMBER, 100, "화면을 구현했습니다.", "일정을 잘 지켰습니다.")
        ));
        PeerEvaluationResponseRequest request = new PeerEvaluationResponseRequest(
            "백엔드 API를 구현했습니다.",
            "협업이 원활했습니다.",
            List.of(
                new PeerEvaluationAnswerRequest(
                    "TEAMMATE_CONTRIBUTION", ACTIVE_MEMBER, 100, "화면을 구현했습니다.", "일정을 잘 지켰습니다.", null
                ),
                new PeerEvaluationAnswerRequest("REFLECTION", null, null, null, null, "다음에는 일정을 더 일찍 정하겠습니다.")
            ),
            true
        );

        var response = facade.submitResponse(FORM_ID, REQUESTER, request);

        assertThat(response.status()).isEqualTo(PeerEvaluationSubmissionStatus.SUBMITTED);
        assertThat(response.submittedAt()).isNotNull();
        assertThat(response.answers()).hasSize(2);
        then(teammateAnswerRepository).should().deleteAllBySubmissionId(10L);
        then(teammateAnswerRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("최종 제출의 팀원 기여도 합계가 100이 아니면 거부한다")
    void rejectsInvalidContributionSum() {
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.STUDENT, Status.ACTIVE)));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(member(REQUESTER, true, "개발")));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(
            member(REQUESTER, true, "개발"), member(ACTIVE_MEMBER, false, "디자인")
        ));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ACTIVE_MEMBER))
            .willReturn(Optional.of(enrollment(ACTIVE_MEMBER, Role.STUDENT, Status.ACTIVE)));
        PeerEvaluationResponseRequest request = new PeerEvaluationResponseRequest(
            "백엔드 API를 구현했습니다.", "협업이 원활했습니다.",
            List.of(
                new PeerEvaluationAnswerRequest(
                    "TEAMMATE_CONTRIBUTION", ACTIVE_MEMBER, 90, "화면을 구현했습니다.", "일정을 잘 지켰습니다.", null
                ),
                new PeerEvaluationAnswerRequest("REFLECTION", null, null, null, null, "회고")
            ), true
        );

        assertThatThrownBy(() -> facade.submitResponse(FORM_ID, REQUESTER, request))
            .isInstanceOf(InvalidPeerEvaluationResponseException.class);

        then(submissionRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상호평가 기간이 아니면 임시저장도 거부한다")
    void rejectsDraftOutsideWindow() {
        given(formRepository.findById(FORM_ID)).willReturn(Optional.of(closedForm()));
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.STUDENT, Status.ACTIVE)));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(member(REQUESTER, true, "개발")));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(member(REQUESTER, true, "개발")));

        PeerEvaluationResponseRequest request = new PeerEvaluationResponseRequest("", "", List.of(), false);

        assertThatThrownBy(() -> facade.submitResponse(FORM_ID, REQUESTER, request))
            .isInstanceOf(PeerEvaluationClosedException.class);
    }

    @Test
    @DisplayName("임시저장에서도 팀원 기여도 범위를 벗어난 값은 거부한다")
    void rejectsOutOfRangeContributionOnDraft() {
        given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(enrollment(REQUESTER, Role.STUDENT, Status.ACTIVE)));
        given(teamMemberRepository.findActiveBySectionIdAndUserId(SECTION_ID, REQUESTER))
            .willReturn(Optional.of(member(REQUESTER, true, "개발")));
        given(teamMemberRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of(
            member(REQUESTER, true, "개발"), member(ACTIVE_MEMBER, false, "디자인")
        ));
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ACTIVE_MEMBER))
            .willReturn(Optional.of(enrollment(ACTIVE_MEMBER, Role.STUDENT, Status.ACTIVE)));
        PeerEvaluationResponseRequest request = new PeerEvaluationResponseRequest(
            "", "",
            List.of(new PeerEvaluationAnswerRequest(
                "TEAMMATE_CONTRIBUTION", ACTIVE_MEMBER, 101, "", "", null
            )), false
        );

        assertThatThrownBy(() -> facade.submitResponse(FORM_ID, REQUESTER, request))
            .isInstanceOf(InvalidPeerEvaluationResponseException.class);
    }

    private PeerEvaluationForm form() {
        return PeerEvaluationForm.restore(
            FORM_ID,
            SECTION_ID,
            MILESTONE_ID,
            false,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            null
        );
    }

    private PeerEvaluationForm closedForm() {
        return PeerEvaluationForm.restore(
            FORM_ID, SECTION_ID, MILESTONE_ID, false,
            LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), null, null, null
        );
    }

    private Milestone milestone() {
        return Milestone.restore(
            MILESTONE_ID,
            SECTION_ID,
            "상호평가",
            "팀 기여도 평가",
            12,
            MilestoneStatus.PUBLISHED,
            new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
            MilestoneType.GENERAL,
            false
        );
    }

    private Milestone milestone(Long id, MilestoneType type, MilestoneStatus status, int weekNumber) {
        return Milestone.restore(
            id,
            SECTION_ID,
            type.name(),
            "평가",
            weekNumber,
            status,
            new MilestoneSchedule(null, LocalDateTime.now().plusDays(1), null, null, null, null),
            type,
            false
        );
    }

    private PeerEvaluationForm peerEvaluationForm(Long id, Long milestoneId) {
        return PeerEvaluationForm.restore(
            id,
            SECTION_ID,
            milestoneId,
            false,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1),
            null,
            null,
            null
        );
    }

    private TeamMember member(String userId, boolean leader, String projectRole) {
        return TeamMember.builder().id(1L).teamId(TEAM_ID).userId(userId).isLeader(leader).projectRole(projectRole).build();
    }

    private Enrollment enrollment(String userId, Role role, Status status) {
        return Enrollment.builder().id(1L).sectionId(SECTION_ID).userId(userId).role(role).status(status).build();
    }

    private User user(String userId, String name, UserGlobalRole role) {
        return User.builder().studentNumber(userId).name(name).globalRole(role).build();
    }
}
