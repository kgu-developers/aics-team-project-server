package milestone.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.milestone.application.MilestoneFacade;
import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneEvaluationWindowRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneScheduleRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneStatusRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneUpdateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest.MilestoneWeekNumberItem;
import kgu.developers.domain.milestone.application.command.MilestoneCommandService;
import kgu.developers.domain.milestone.application.command.MilestoneWeekNumberChange;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
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

    @InjectMocks
    private MilestoneFacade milestoneFacade;

    @Test
    @DisplayName("생성 요청을 도메인 값으로 변환하고 생성된 식별자를 응답한다")
    void createMilestone() {
        MilestoneCreateRequest request = new MilestoneCreateRequest(
                "제안서",
                "제안서 제출",
                2,
                scheduleRequest()
        );
        given(milestoneCommandService.createMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                "제안서",
                "제안서 제출",
                2,
                schedule()
        )).willReturn(MILESTONE_ID);

        assertThat(milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request).id())
                .isEqualTo(MILESTONE_ID);
    }

    @Test
    @DisplayName("분반과 식별자로 상세를 조회해 응답 DTO로 변환한다")
    void getMilestone() {
        Milestone milestone = milestone();
        given(milestoneQueryService.getMilestone(SECTION_ID, MILESTONE_ID)).willReturn(milestone);

        assertThat(milestoneFacade.getMilestone(SECTION_ID, MILESTONE_ID).title()).isEqualTo("제안서");
        assertThat(milestoneFacade.getMilestone(SECTION_ID, MILESTONE_ID).schedule().dueAt()).isEqualTo(DUE_AT);
    }

    @Test
    @DisplayName("상세와 일정 수정에 분반 경계를 포함한다")
    void updateMilestone() {
        MilestoneUpdateRequest request = new MilestoneUpdateRequest("중간보고서", null, scheduleRequest());

        milestoneFacade.updateMilestone(SECTION_ID, PROFESSOR_ID, MILESTONE_ID, request);

        verify(milestoneCommandService).updateMilestone(
                SECTION_ID,
                PROFESSOR_ID,
                MILESTONE_ID,
                "중간보고서",
                null,
                schedule()
        );
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
                scheduleRequest()
        );
        willThrow(new DuplicateMilestoneWeekException())
                .given(milestoneCommandService)
                .createMilestone(
                        SECTION_ID,
                        PROFESSOR_ID,
                        "제안서",
                        "제안서 제출",
                        2,
                        schedule()
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
        MilestoneCreateRequest request = new MilestoneCreateRequest("제안서", null, 2, invalidSchedule);

        assertThatThrownBy(() -> milestoneFacade.createMilestone(SECTION_ID, PROFESSOR_ID, request))
                .isInstanceOf(InvalidMilestoneRequestException.class);
    }

    @Test
    @DisplayName("조회 서비스의 잘못된 입력은 잘못된 요청 예외로 변환한다")
    void invalidQueryInput() {
        given(milestoneQueryService.getMilestone(SECTION_ID, 0L))
                .willThrow(new IllegalArgumentException("마일스톤 식별자는 양수여야 합니다."));

        assertThatThrownBy(() -> milestoneFacade.getMilestone(SECTION_ID, 0L))
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
