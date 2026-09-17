package milestone.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.admin.milestone.application.MilestoneAccessValidator;
import kgu.developers.admin.milestone.application.MilestoneFacade;
import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneEvaluationWindowRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneScheduleRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneStatusRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneUpdateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest.MilestoneWeekNumberItem;
import kgu.developers.admin.milestone.presentation.request.RequiredArtifactRequest;
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneResponse;
import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import kgu.developers.domain.feedback.application.command.RequiredArtifactCommandService;
import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.milestone.application.command.MilestoneCommandService;
import kgu.developers.domain.milestone.application.command.MilestoneWeekNumberChange;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.milestone.exception.DuplicateMilestoneWeekException;
import kgu.developers.domain.milestone.exception.InvalidMilestoneRequestException;
import kgu.developers.domain.milestone.exception.MilestoneConcurrentlyModifiedException;

@ExtendWith(MockitoExtension.class)
class MilestoneFacadeTest {
    private static final Long SECTION_ID = 1L;
    private static final Long MILESTONE_ID = 2L;
    private static final String PROFESSOR_ID = "20260001";
    private static final LocalDateTime DUE_AT = LocalDateTime.of(2026, 9, 10, 23, 59);

    @Mock
    private MilestoneCommandService milestoneCommandService;

    @Mock
    private MilestoneQueryService milestoneQueryService;

    @Mock
    private MilestoneAccessValidator milestoneAccessValidator;

    @Mock
    private PeerEvaluationFormCommandService peerEvaluationFormCommandService;

    @Mock
    private PeerEvaluationFormRepository peerEvaluationFormRepository;

    @Mock
    private RequiredArtifactCommandService requiredArtifactCommandService;

    @Mock
    private RequiredArtifactQueryService requiredArtifactQueryService;

    @InjectMocks
    private MilestoneFacade milestoneFacade;

    @Test
    @DisplayName("생성 요청을 도메인 값으로 변환하고 생성된 식별자를 응답한다")
    void createMilestone() {
        MilestoneCreateRequest request = new MilestoneCreateRequest(
                "제안서",
                "제안서 제출",
                2,
                scheduleRequest(),
                null
        );
        given(milestoneCommandService.createMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                "제안서",
                "제안서 제출",
                2,
                schedule(),
                null,
                false
        )).willReturn(MILESTONE_ID);

        assertThat(milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request).id())
                .isEqualTo(MILESTONE_ID);
    }

    @Test
    @DisplayName("생성 요청에 마일스톤 유형을 지정하면 그대로 전달된다")
    void createMilestoneWithType() {
        MilestoneCreateRequest request = new MilestoneCreateRequest(
                "최종보고서",
                "최종보고서 제출",
                15,
                scheduleRequest(),
                MilestoneType.FINAL_REPORT
        );
        given(milestoneCommandService.createMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                "최종보고서",
                "최종보고서 제출",
                15,
                schedule(),
                MilestoneType.FINAL_REPORT,
                false
        )).willReturn(MILESTONE_ID);

        assertThat(milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request).id())
                .isEqualTo(MILESTONE_ID);
    }

    @Test
    @DisplayName("생성 요청의 마감 전 재제출 허용 설정을 도메인 명령에 전달한다")
    void createMilestoneWithResubmissionPolicy() {
        MilestoneCreateRequest request = new MilestoneCreateRequest(
                "제안서",
                "제안서 제출",
                2,
                scheduleRequest(),
                MilestoneType.PROPOSAL,
                true
        );
        given(milestoneCommandService.createMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                "제안서",
                "제안서 제출",
                2,
                schedule(),
                MilestoneType.PROPOSAL,
                true
        )).willReturn(MILESTONE_ID);

        assertThat(milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request).id())
                .isEqualTo(MILESTONE_ID);
    }

    @Test
    @DisplayName("분반과 식별자로 상세를 조회해 응답 DTO로 변환한다")
    void getMilestone() {
        Milestone milestone = milestone();
        given(milestoneQueryService.getMilestone(SECTION_ID, MILESTONE_ID)).willReturn(milestone);

        MilestoneResponse response =
                milestoneFacade.getMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID);

        assertThat(response.title()).isEqualTo("제안서");
        assertThat(response.schedule().dueAt()).isEqualTo(DUE_AT);
        assertThat(response.allowResubmissionBeforeDueAt()).isFalse();
        assertThat(response.peerEvaluationForm()).isNull();
        verify(milestoneAccessValidator).validateSectionAccess(SECTION_ID, PROFESSOR_ID);
    }

    @Test
    @DisplayName("상호평가 마일스톤 상세 조회 시 양식 정보가 포함된다")
    void getPeerEvaluationMilestone() {
        Milestone peerEvalMilestone = Milestone.restore(
                MILESTONE_ID,
                SECTION_ID,
                "동료평가",
                null,
                3,
                MilestoneStatus.DRAFT,
                schedule(),
                MilestoneType.PEER_EVALUATION,
                false
        );
        LocalDateTime opensAt = DUE_AT.minusDays(5);
        PeerEvaluationForm form = PeerEvaluationForm.restore(
                10L, SECTION_ID, MILESTONE_ID, true, opensAt, DUE_AT, null, null, null);
        given(milestoneQueryService.getMilestone(SECTION_ID, MILESTONE_ID)).willReturn(peerEvalMilestone);
        given(peerEvaluationFormRepository.findByMilestoneId(MILESTONE_ID)).willReturn(java.util.Optional.of(form));

        MilestoneResponse response =
                milestoneFacade.getMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID);

        assertThat(response.type()).isEqualTo(MilestoneType.PEER_EVALUATION);
        assertThat(response.peerEvaluationForm()).isNotNull();
        assertThat(response.peerEvaluationForm().id()).isEqualTo(10L);
        assertThat(response.peerEvaluationForm().anonymous()).isTrue();
        assertThat(response.peerEvaluationForm().opensAt()).isEqualTo(opensAt);
        assertThat(response.peerEvaluationForm().closesAt()).isEqualTo(DUE_AT);
    }

    @Test
    @DisplayName("담당 교수가 아니면 마일스톤을 조회하지 않는다")
    void rejectQueryByAnotherProfessor() {
        willThrow(new AccessDeniedException("담당 분반만 접근할 수 있습니다."))
                .given(milestoneAccessValidator)
                .validateSectionAccess(SECTION_ID, PROFESSOR_ID);

        assertThatThrownBy(() ->
                milestoneFacade.getMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID))
                .isInstanceOf(AccessDeniedException.class);

        then(milestoneQueryService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상호평가가 포함되지 않은 마일스톤 목록 조회는 양식 조회 없이 반환한다")
    void getMilestonesWithoutPeerEvaluation() {
        Milestone milestone = milestone();
        given(milestoneQueryService.getMilestones(SECTION_ID, MilestoneStatus.DRAFT))
                .willReturn(List.of(milestone));

        MilestoneListResponse response =
                milestoneFacade.getMilestones(SECTION_ID, PROFESSOR_ID, MilestoneStatus.DRAFT);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("제안서");
        verify(milestoneAccessValidator).validateSectionAccess(SECTION_ID, PROFESSOR_ID);
        then(peerEvaluationFormRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상호평가가 포함된 마일스톤 목록 조회 시 양식 일정을 매핑한다")
    void getMilestonesWithPeerEvaluation() {
        Milestone normalMilestone = milestone();
        Milestone peerEvalMilestone = Milestone.restore(
                3L,
                SECTION_ID,
                "동료평가",
                null,
                3,
                MilestoneStatus.DRAFT,
                schedule(),
                MilestoneType.PEER_EVALUATION,
                false
        );
        given(milestoneQueryService.getMilestones(SECTION_ID, null))
                .willReturn(List.of(normalMilestone, peerEvalMilestone));

        LocalDateTime opensAt = DUE_AT.minusDays(5);
        PeerEvaluationForm form = PeerEvaluationForm.restore(
                10L, SECTION_ID, 3L, true, opensAt, DUE_AT, null, null, null);
        given(peerEvaluationFormRepository.findAllBySectionIdOrderByIdDesc(SECTION_ID))
                .willReturn(List.of(form));

        MilestoneListResponse response = milestoneFacade.getMilestones(SECTION_ID, PROFESSOR_ID, null);

        assertThat(response.content()).hasSize(2);
        MilestoneResponse peerResponse = response.content().stream()
                .filter(m -> m.id().equals(3L))
                .findFirst().orElseThrow();
        assertThat(peerResponse.schedule().opensAt()).isEqualTo(opensAt);
        assertThat(peerResponse.schedule().evaluationOpensAt()).isEqualTo(opensAt);
        assertThat(peerResponse.schedule().evaluationClosesAt()).isEqualTo(DUE_AT);
        assertThat(peerResponse.peerEvaluationForm()).isNotNull();
        assertThat(peerResponse.peerEvaluationForm().id()).isEqualTo(10L);
        assertThat(peerResponse.peerEvaluationForm().anonymous()).isTrue();
        assertThat(peerResponse.peerEvaluationForm().opensAt()).isEqualTo(opensAt);
        assertThat(peerResponse.peerEvaluationForm().closesAt()).isEqualTo(DUE_AT);
    }

    @Test
    @DisplayName("상세와 일정 수정에 분반 경계를 포함한다")
    void updateMilestone() {
        MilestoneUpdateRequest request = new MilestoneUpdateRequest("중간보고서", null, scheduleRequest(), null);

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "중간보고서",
                null,
                schedule(),
                null,
                null
        );
    }

    @Test
    @DisplayName("수정 요청에 마일스톤 유형을 지정하면 그대로 전달된다")
    void updateMilestoneWithType() {
        MilestoneUpdateRequest request =
                new MilestoneUpdateRequest("중간보고서", null, scheduleRequest(), MilestoneType.MID_REPORT);

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "중간보고서",
                null,
                schedule(),
                MilestoneType.MID_REPORT,
                null
        );
    }

    @Test
    @DisplayName("수정 요청의 마감 전 재제출 허용 설정을 도메인 명령에 전달한다")
    void updateMilestoneWithResubmissionPolicy() {
        MilestoneUpdateRequest request = new MilestoneUpdateRequest(
                "중간보고서",
                null,
                scheduleRequest(),
                MilestoneType.MID_REPORT,
                true
        );

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "중간보고서",
                null,
                schedule(),
                MilestoneType.MID_REPORT,
                true
        );
    }

    @Test
    @DisplayName("상호 평가 마일스톤 수정 시 상호평가 양식의 기간도 함께 갱신된다")
    void updateMilestoneForPeerEvaluation() {
        LocalDateTime opensAt = DUE_AT.minusDays(7);
        MilestoneScheduleRequest scheduleRequest = new MilestoneScheduleRequest(
                opensAt,
                DUE_AT,
                null,
                null,
                null,
                null
        );
        MilestoneUpdateRequest request = new MilestoneUpdateRequest(
                "상호 평가",
                "팀원 상호 평가",
                scheduleRequest,
                MilestoneType.PEER_EVALUATION,
                false
        );

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        MilestoneSchedule expectedSchedule = new MilestoneSchedule(opensAt, DUE_AT, null, null, null, null);
        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "상호 평가",
                "팀원 상호 평가",
                expectedSchedule,
                MilestoneType.PEER_EVALUATION,
                false
        );
        verify(peerEvaluationFormCommandService).updateFormByMilestoneId(
                SECTION_ID,
                MILESTONE_ID,
                null,
                opensAt,
                DUE_AT
        );
    }

    @Test
    @DisplayName("마일스톤 유형이 생략되어도 기존 상호평가 양식이 존재하면 기간을 갱신한다")
    void updateMilestoneForPeerEvaluationWhenTypeNullAndFormExists() {
        LocalDateTime opensAt = DUE_AT.minusDays(7);
        MilestoneScheduleRequest scheduleRequest = new MilestoneScheduleRequest(
                opensAt,
                DUE_AT,
                null,
                null,
                null,
                null
        );
        MilestoneUpdateRequest request = new MilestoneUpdateRequest(
                "상호 평가",
                "팀원 상호 평가",
                scheduleRequest,
                null,
                null
        );

        PeerEvaluationForm existingForm = PeerEvaluationForm.restore(
                10L, SECTION_ID, MILESTONE_ID, true, opensAt, DUE_AT, null, null, null);
        given(peerEvaluationFormRepository.findByMilestoneId(MILESTONE_ID))
                .willReturn(java.util.Optional.of(existingForm));

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        MilestoneSchedule expectedSchedule = new MilestoneSchedule(opensAt, DUE_AT, null, null, null, null);
        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "상호 평가",
                "팀원 상호 평가",
                expectedSchedule,
                null,
                null
        );
        verify(peerEvaluationFormCommandService).updateFormByMilestoneId(
                SECTION_ID,
                MILESTONE_ID,
                null,
                opensAt,
                DUE_AT
        );
    }

    @Test
    @DisplayName("상호평가 수정 시 opensAt/dueAt과 evaluation 필드가 다르면 evaluation 기간을 우선 채택한다")
    void updateMilestoneForPeerEvaluationPrefersEvaluationPeriodOverStandardPeriod() {
        LocalDateTime standardOpensAt = DUE_AT.minusDays(14);
        LocalDateTime standardDueAt = DUE_AT.minusDays(7);
        LocalDateTime evalOpensAt = DUE_AT.minusDays(5);
        LocalDateTime evalClosesAt = DUE_AT;
        MilestoneScheduleRequest scheduleRequest = new MilestoneScheduleRequest(
                standardOpensAt,
                standardDueAt,
                null,
                null,
                evalOpensAt,
                evalClosesAt
        );
        MilestoneUpdateRequest request = new MilestoneUpdateRequest(
                "상호 평가",
                "수정된 상호 평가",
                scheduleRequest,
                MilestoneType.PEER_EVALUATION,
                false
        );

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        MilestoneSchedule expectedSchedule = new MilestoneSchedule(evalOpensAt, evalClosesAt, null, null, null, null);
        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "상호 평가",
                "수정된 상호 평가",
                expectedSchedule,
                MilestoneType.PEER_EVALUATION,
                false
        );
        verify(peerEvaluationFormCommandService).updateFormByMilestoneId(
                SECTION_ID,
                MILESTONE_ID,
                null,
                evalOpensAt,
                evalClosesAt
        );
    }

    @Test
    @DisplayName("상호평가 수정 요청에 익명 설정이 포함되면 양식의 익명 여부도 함께 갱신된다")
    void updateMilestoneForPeerEvaluationUpdatesAnonymous() {
        LocalDateTime opensAt = DUE_AT.minusDays(7);
        MilestoneScheduleRequest scheduleRequest = new MilestoneScheduleRequest(
                opensAt,
                DUE_AT,
                null,
                null,
                null,
                null
        );
        MilestoneUpdateRequest request = new MilestoneUpdateRequest(
                "상호 평가",
                "실명 상호 평가로 변경",
                scheduleRequest,
                MilestoneType.PEER_EVALUATION,
                false,
                false
        );

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(peerEvaluationFormCommandService).updateFormByMilestoneId(
                SECTION_ID,
                MILESTONE_ID,
                false,
                opensAt,
                DUE_AT
        );
    }

    @Test
    @DisplayName("상호평가 생성 시 opensAt/dueAt과 evaluation 필드가 다르면 evaluation 기간을 우선 채택한다")
    void createMilestoneForPeerEvaluationPrefersEvaluationPeriodOverStandardPeriod() {
        LocalDateTime standardOpensAt = DUE_AT.minusDays(14);
        LocalDateTime standardDueAt = DUE_AT.minusDays(7);
        LocalDateTime evalOpensAt = DUE_AT.minusDays(5);
        LocalDateTime evalClosesAt = DUE_AT;
        MilestoneScheduleRequest scheduleRequest = new MilestoneScheduleRequest(
                standardOpensAt,
                standardDueAt,
                null,
                null,
                evalOpensAt,
                evalClosesAt
        );
        MilestoneCreateRequest request = new MilestoneCreateRequest(
                "상호 평가",
                "신규 상호 평가",
                3,
                scheduleRequest,
                MilestoneType.PEER_EVALUATION,
                false
        );

        MilestoneSchedule expectedSchedule = new MilestoneSchedule(evalOpensAt, evalClosesAt, null, null, null, null);
        given(milestoneCommandService.createMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                "상호 평가",
                "신규 상호 평가",
                3,
                expectedSchedule,
                MilestoneType.PEER_EVALUATION,
                false
        )).willReturn(MILESTONE_ID);

        assertThat(milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request).id())
                .isEqualTo(MILESTONE_ID);
    }

    @Test
    @DisplayName("updateMilestone 메서드에는 원자적 갱신을 보장하기 위해 @Transactional 어노테이션이 존재해야 한다")
    void updateMilestoneHasTransactionalAnnotation() throws NoSuchMethodException {
        var method = MilestoneFacade.class.getMethod(
                "updateMilestone",
                Long.class,
                String.class,
                Long.class,
                MilestoneUpdateRequest.class
        );
        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    @DisplayName("상호평가 마일스톤 수정 시 일정이 null이면 양식 기간을 갱신하지 않는다")
    void updateMilestonePeerEvaluationWithNullSchedule() {
        MilestoneUpdateRequest request = new MilestoneUpdateRequest(
                "상호 평가",
                "팀원 상호 평가",
                null,
                MilestoneType.PEER_EVALUATION,
                false
        );

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "상호 평가",
                "팀원 상호 평가",
                null,
                MilestoneType.PEER_EVALUATION,
                false
        );
        then(peerEvaluationFormCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상호 평가 마일스톤 조회 시 상호평가 양식의 시작 및 종료 시각이 일정 응답에 채워진다")
    void getMilestoneForPeerEvaluation() {
        LocalDateTime opensAt = DUE_AT.minusDays(7);
        Milestone peerEvaluationMilestone = Milestone.restore(
                MILESTONE_ID,
                SECTION_ID,
                "상호 평가",
                null,
                14,
                MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, DUE_AT, null, null, null, null),
                MilestoneType.PEER_EVALUATION,
                false
        );
        PeerEvaluationForm form = PeerEvaluationForm.restore(
                10L, SECTION_ID, MILESTONE_ID, true, opensAt, DUE_AT, null, null, null);

        given(milestoneQueryService.getMilestone(SECTION_ID, MILESTONE_ID)).willReturn(peerEvaluationMilestone);
        given(peerEvaluationFormRepository.findByMilestoneId(MILESTONE_ID)).willReturn(java.util.Optional.of(form));

        MilestoneResponse response = milestoneFacade.getMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID);

        assertThat(response.type()).isEqualTo(MilestoneType.PEER_EVALUATION);
        assertThat(response.schedule().opensAt()).isEqualTo(opensAt);
        assertThat(response.schedule().dueAt()).isEqualTo(DUE_AT);
        assertThat(response.schedule().evaluationOpensAt()).isEqualTo(opensAt);
        assertThat(response.schedule().evaluationClosesAt()).isEqualTo(DUE_AT);
    }

    @Test
    @DisplayName("공개 상태 변경에 분반 경계를 포함한다")
    void changeStatus() {
        milestoneFacade.changeStatus(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                new MilestoneStatusRequest(MilestoneStatus.PUBLISHED)
        );

        verify(milestoneCommandService)
                .changeStatus(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, MilestoneStatus.PUBLISHED);
    }

    @Test
    @DisplayName("평가 기간 수정에 분반 경계와 요청 시각을 포함한다")
    void updateEvaluationWindow() {
        LocalDateTime evaluationOpensAt = DUE_AT.plusDays(1);
        LocalDateTime evaluationClosesAt = DUE_AT.plusDays(3);
        MilestoneEvaluationWindowRequest request = new MilestoneEvaluationWindowRequest(
                evaluationOpensAt,
                evaluationClosesAt,
                false
        );

        milestoneFacade.updateEvaluationWindow(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(milestoneCommandService).updateEvaluationWindow(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                evaluationOpensAt,
                evaluationClosesAt
        );
    }

    @Test
    @DisplayName("잘못된 평가 기간은 잘못된 요청 예외로 변환한다")
    void invalidEvaluationWindow() {
        LocalDateTime evaluationOpensAt = DUE_AT.plusDays(3);
        LocalDateTime evaluationClosesAt = DUE_AT.plusDays(1);
        MilestoneEvaluationWindowRequest request = new MilestoneEvaluationWindowRequest(
                evaluationOpensAt,
                evaluationClosesAt,
                false
        );
        willThrow(new IllegalArgumentException("평가 시작 시각은 종료 시각보다 빨라야 합니다."))
                .given(milestoneCommandService)
                .updateEvaluationWindow(
                        SECTION_ID,
                        PROFESSOR_ID,
                        MILESTONE_ID,
                        evaluationOpensAt,
                        evaluationClosesAt
                );

        assertThatThrownBy(() -> milestoneFacade.updateEvaluationWindow(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                request
        ))
                .isInstanceOf(InvalidMilestoneRequestException.class);
    }

    @Test
    @DisplayName("명시적인 해제 요청은 평가 기간을 null로 변경한다")
    void clearEvaluationWindow() {
        MilestoneEvaluationWindowRequest request = new MilestoneEvaluationWindowRequest(
                null,
                null,
                true
        );

        milestoneFacade.updateEvaluationWindow(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(milestoneCommandService).updateEvaluationWindow(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                null,
                null
        );
    }

    @Test
    @DisplayName("도메인에서 발생한 주차 충돌 예외를 그대로 전달한다")
    void duplicateWeekNumber() {
        MilestoneCreateRequest request = new MilestoneCreateRequest(
                "제안서",
                "제안서 제출",
                2,
                scheduleRequest(),
                null
        );
        willThrow(new DuplicateMilestoneWeekException())
                .given(milestoneCommandService)
                .createMilestone(
                        SECTION_ID,
                        PROFESSOR_ID,
                        "제안서",
                        "제안서 제출",
                        2,
                        schedule(),
                        null,
                        false
                );

        assertThatThrownBy(() -> milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request))
                .isInstanceOf(DuplicateMilestoneWeekException.class);
    }

    @Test
    @DisplayName("주차 변경 요청을 도메인 명령으로 변환한다")
    void updateWeekNumbers() {
        MilestoneWeekNumbersRequest request = new MilestoneWeekNumbersRequest(List.of(
                new MilestoneWeekNumberItem(MILESTONE_ID, 3)
        ));

        milestoneFacade.updateWeekNumbers(SECTION_ID, PROFESSOR_ID, request);

        verify(milestoneCommandService).updateWeekNumbers(
                SECTION_ID,
                PROFESSOR_ID,
                List.of(new MilestoneWeekNumberChange(MILESTONE_ID, 3))
        );
    }

    @Test
    @DisplayName("동시 주차 변경 충돌은 잘못된 사용자 요청으로 변환하지 않는다")
    void preservesConcurrentWeekNumberChangeFailure() {
        MilestoneWeekNumbersRequest request = new MilestoneWeekNumbersRequest(List.of(
                new MilestoneWeekNumberItem(MILESTONE_ID, 3)
        ));
        MilestoneConcurrentlyModifiedException failure =
                new MilestoneConcurrentlyModifiedException();
        willThrow(failure)
                .given(milestoneCommandService)
                .updateWeekNumbers(
                        SECTION_ID,
                        PROFESSOR_ID,
                        List.of(new MilestoneWeekNumberChange(MILESTONE_ID, 3))
                );

        assertThatThrownBy(() -> milestoneFacade.updateWeekNumbers(
                SECTION_ID,
                PROFESSOR_ID,
                request
        )).isSameAs(failure);
    }

    @Test
    @DisplayName("순서가 잘못된 일정은 잘못된 요청 예외로 변환한다")
    void invalidSchedule() {
        MilestoneScheduleRequest invalidSchedule = new MilestoneScheduleRequest(
                DUE_AT.plusDays(1),
                DUE_AT,
                null,
                null,
                null,
                null
        );
        MilestoneCreateRequest request = new MilestoneCreateRequest("제안서", null, 2, invalidSchedule, null);

        assertThatThrownBy(() -> milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request))
                .isInstanceOf(InvalidMilestoneRequestException.class);
    }

    @Test
    @DisplayName("조회 서비스의 잘못된 입력은 잘못된 요청 예외로 변환한다")
    void invalidQueryInput() {
        given(milestoneQueryService.getMilestone(SECTION_ID, 0L))
                .willThrow(new IllegalArgumentException("마일스톤 식별자는 양수여야 합니다."));

        assertThatThrownBy(() -> milestoneFacade.getMilestone(SECTION_ID, PROFESSOR_ID, 0L))
                .isInstanceOf(InvalidMilestoneRequestException.class);
    }

    @Test
    @DisplayName("명령 서비스의 잘못된 입력은 잘못된 요청 예외로 변환한다")
    void invalidCommandInput() {
        MilestoneStatusRequest request = new MilestoneStatusRequest(null);
        willThrow(new IllegalArgumentException("공개 상태는 필수입니다."))
                .given(milestoneCommandService)
                .changeStatus(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, null);

        assertThatThrownBy(() -> milestoneFacade.changeStatus(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                request
        ))
                .isInstanceOf(InvalidMilestoneRequestException.class);
    }

    private Milestone milestone() {
        return Milestone.restore(
                MILESTONE_ID,
                SECTION_ID,
                "제안서",
                null,
                2,
                MilestoneStatus.DRAFT,
                schedule()
        );
    }

    private MilestoneScheduleRequest scheduleRequest() {
        return new MilestoneScheduleRequest(null, DUE_AT, null, null, null, null);
    }

    private MilestoneSchedule schedule() {
        return new MilestoneSchedule(null, DUE_AT, null, null, null, null);
    }

}
