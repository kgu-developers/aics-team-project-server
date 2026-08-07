package milestone.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.milestone.domain.MilestoneSchedule;

class MilestoneScheduleTest {
    private static final LocalDateTime DUE_AT = LocalDateTime.of(2026, 9, 10, 23, 59);

    @Test
    @DisplayName("마감 시각은 필수이다")
    void dueAtIsRequired() {
        assertThatThrownBy(() -> new MilestoneSchedule(null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("마감 시각");
    }

    @Test
    @DisplayName("선택 기간 없이 마감 시각만으로 일정을 생성할 수 있다")
    void createWithDueAtOnly() {
        MilestoneSchedule schedule = new MilestoneSchedule(null, DUE_AT, null, null, null, null);

        assertThat(schedule.dueAt()).isEqualTo(DUE_AT);
        assertThat(schedule.opensAt()).isNull();
    }

    @Test
    @DisplayName("작성 시작 시각은 마감 시각보다 빨라야 한다")
    void opensAtMustBeBeforeDueAt() {
        assertThatThrownBy(() -> new MilestoneSchedule(DUE_AT, DUE_AT, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("작성 시작 시각");
    }

    @Test
    @DisplayName("지각 제출 종료 시각은 마감 시각보다 빠를 수 없다")
    void lateSubmissionUntilCannotBeBeforeDueAt() {
        assertThatThrownBy(() -> new MilestoneSchedule(
                null,
                DUE_AT,
                DUE_AT.minusMinutes(1),
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지각 제출 종료 시각");
    }

    @Test
    @DisplayName("수정 종료 시각은 마감 시각보다 빠를 수 없다")
    void revisionUntilCannotBeBeforeDueAt() {
        assertThatThrownBy(() -> new MilestoneSchedule(
                null,
                DUE_AT,
                null,
                DUE_AT.minusMinutes(1),
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("수정 종료 시각");
    }

    @Test
    @DisplayName("수정 종료 시각은 지각 제출 종료 시각보다 빠를 수 없다")
    void revisionUntilCannotBeBeforeLateSubmissionUntil() {
        assertThatThrownBy(() -> new MilestoneSchedule(
                null,
                DUE_AT,
                DUE_AT.plusDays(2),
                DUE_AT.plusDays(1),
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지각 제출 종료 시각");
    }

    @Test
    @DisplayName("평가 시작 시각과 종료 시각은 함께 설정해야 한다")
    void evaluationWindowMustBeComplete() {
        assertThatThrownBy(() -> new MilestoneSchedule(
                null,
                DUE_AT,
                null,
                null,
                DUE_AT.plusDays(1),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("함께 설정");
    }

    @Test
    @DisplayName("평가 시작 시각은 마감 시각보다 빠를 수 없다")
    void evaluationStartCannotBeBeforeDueAt() {
        assertThatThrownBy(() -> new MilestoneSchedule(
                null,
                DUE_AT,
                null,
                null,
                DUE_AT.minusMinutes(1),
                DUE_AT.plusDays(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("마감 시각");
    }

    @Test
    @DisplayName("평가 시작 시각은 종료 시각보다 빨라야 한다")
    void evaluationStartMustBeBeforeEnd() {
        LocalDateTime evaluationAt = DUE_AT.plusDays(1);

        assertThatThrownBy(() -> new MilestoneSchedule(
                null,
                DUE_AT,
                null,
                null,
                evaluationAt,
                evaluationAt
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("평가 시작 시각");
    }

    @Test
    @DisplayName("마감 이후의 지각 제출, 수정, 평가 기간을 함께 설정할 수 있다")
    void createFullSchedule() {
        MilestoneSchedule schedule = new MilestoneSchedule(
                DUE_AT.minusDays(7),
                DUE_AT,
                DUE_AT.plusDays(1),
                DUE_AT.plusDays(2),
                DUE_AT.plusDays(3),
                DUE_AT.plusDays(4)
        );

        assertThat(schedule.lateSubmissionUntil()).isEqualTo(DUE_AT.plusDays(1));
        assertThat(schedule.revisionUntil()).isEqualTo(DUE_AT.plusDays(2));
        assertThat(schedule.evaluationClosesAt()).isEqualTo(DUE_AT.plusDays(4));
    }
}
