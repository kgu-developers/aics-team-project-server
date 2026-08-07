package milestone.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.infrastructure.MilestoneJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestoneJpaEntityTest {

    @Test
    @DisplayName("마일스톤 도메인과 JPA 엔티티를 변환해도 일정 정보가 유지된다")
    void keepsScheduleDuringRoundTripMapping() {
        MilestoneSchedule schedule = schedule();
        Milestone milestone = Milestone.restore(
                3L,
                7L,
                "프로젝트 제안서",
                "제안서를 제출합니다.",
                2,
                MilestoneStatus.PUBLISHED,
                schedule
        );

        Milestone restored = MilestoneJpaEntity.fromDomain(milestone).toDomain();

        assertThat(restored.getId()).isEqualTo(3L);
        assertThat(restored.getSectionId()).isEqualTo(7L);
        assertThat(restored.getTitle()).isEqualTo("프로젝트 제안서");
        assertThat(restored.getDescription()).isEqualTo("제안서를 제출합니다.");
        assertThat(restored.getWeekNumber()).isEqualTo(2);
        assertThat(restored.getStatus()).isEqualTo(MilestoneStatus.PUBLISHED);
        assertThat(restored.getSchedule()).isEqualTo(schedule);
    }

    private MilestoneSchedule schedule() {
        return new MilestoneSchedule(
                LocalDateTime.of(2026, 8, 10, 9, 0),
                LocalDateTime.of(2026, 8, 17, 23, 59),
                LocalDateTime.of(2026, 8, 18, 23, 59),
                LocalDateTime.of(2026, 8, 19, 23, 59),
                LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 21, 18, 0)
        );
    }
}
