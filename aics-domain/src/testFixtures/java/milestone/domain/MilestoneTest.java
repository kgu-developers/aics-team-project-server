package milestone.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;

class MilestoneTest {
    @Test
    @DisplayName("새 마일스톤은 작성중 상태로 생성된다")
    void createAsDraft() {
        Milestone milestone = Milestone.create(1L, "제안서", "프로젝트 제안서", 2, schedule());

        assertThat(milestone.getSectionId()).isEqualTo(1L);
        assertThat(milestone.getTitle()).isEqualTo("제안서");
        assertThat(milestone.getWeekNumber()).isEqualTo(2);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.DRAFT);
    }

    @Test
    @DisplayName("제목은 공백일 수 없다")
    void titleCannotBeBlank() {
        assertThatThrownBy(() -> Milestone.create(1L, "  ", null, 1, schedule()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("제목");
    }

    @Test
    @DisplayName("제목은 영속성 제한인 100자를 초과할 수 없다")
    void titleCannotExceedPersistenceLimit() {
        Milestone milestone = Milestone.create(1L, "a".repeat(100), null, 1, schedule());

        assertThat(milestone.getTitle()).hasSize(100);
        assertThatThrownBy(() -> Milestone.create(1L, "a".repeat(101), null, 1, schedule()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100자");
    }

    @Test
    @DisplayName("분반 식별자와 주차는 양수여야 한다")
    void sectionIdAndWeekNumberMustBePositive() {
        assertThatThrownBy(() -> Milestone.create(0L, "제안서", null, 1, schedule()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("분반 식별자");

        assertThatThrownBy(() -> Milestone.create(1L, "제안서", null, 0, schedule()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주차");
    }

    @Test
    @DisplayName("상세 내용, 일정, 주차, 공개 상태를 변경할 수 있다")
    void updateMilestone() {
        Milestone milestone = Milestone.create(1L, "제안서", null, 1, schedule());
        MilestoneSchedule updatedSchedule = new MilestoneSchedule(
                LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 9, 20, 23, 59),
                null,
                null,
                null,
                null
        );

        milestone.updateDetails("중간보고서", "중간 진행 상황 제출");
        milestone.updateSchedule(updatedSchedule);
        milestone.changeWeekNumber(5);
        milestone.changeStatus(MilestoneStatus.PUBLISHED);

        assertThat(milestone.getTitle()).isEqualTo("중간보고서");
        assertThat(milestone.getDescription()).isEqualTo("중간 진행 상황 제출");
        assertThat(milestone.getSchedule()).isEqualTo(updatedSchedule);
        assertThat(milestone.getWeekNumber()).isEqualTo(5);
        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.PUBLISHED);
    }

    @Test
    @DisplayName("마일스톤이 특정 분반에 속하는지 확인할 수 있다")
    void belongsToSection() {
        Milestone milestone = Milestone.create(10L, "제안서", null, 1, schedule());

        assertThat(milestone.belongsToSection(10L)).isTrue();
        assertThat(milestone.belongsToSection(11L)).isFalse();
    }

    private MilestoneSchedule schedule() {
        return new MilestoneSchedule(null, LocalDateTime.of(2026, 9, 10, 23, 59), null, null, null, null);
    }
}
