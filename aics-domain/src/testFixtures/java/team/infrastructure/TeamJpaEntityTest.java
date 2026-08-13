package team.infrastructure;

import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PropertyPath;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TeamJpaEntityTest {

    private final SectionJpaEntity section = SectionJpaEntity.builder().id(10L).build();

    @Test
    @DisplayName("Team <-> TeamJpaEntity 양방향 변환 시 필드가 보존된다")
    void roundTrip() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        Team origin = Team.builder()
                .id(1L)
                .sectionId(10L)
                .name("스프링 A팀")
                .kickoffRule("매일 데일리 스크럼")
                .meetingSchedule("매주 금요일 16시")
                .status(Status.CONFIRMED)
                .createdAt(createdAt)
                .build();

        TeamJpaEntity entity = TeamJpaEntity.toEntity(origin, section);
        Team restored = entity.toDomain();

        assertThat(restored.getId()).isEqualTo(origin.getId());
        assertThat(restored.getSectionId()).isEqualTo(origin.getSectionId());
        assertThat(restored.getName()).isEqualTo(origin.getName());
        assertThat(restored.getKickoffRule()).isEqualTo(origin.getKickoffRule());
        assertThat(restored.getMeetingSchedule()).isEqualTo(origin.getMeetingSchedule());
        assertThat(restored.getStatus()).isEqualTo(origin.getStatus());
        assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("toEntity 호출 시 삭제일(deletedAt) 시각이 올바르게 전달된다")
    void carriesDeletedAt() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 5, 1, 12, 0);
        Team team = Team.builder()
                .id(1L)
                .sectionId(10L)
                .name("팀1")
                .kickoffRule("규칙")
                .meetingSchedule("일정")
                .status(Status.FORMING)
                .deletedAt(deletedAt)
                .build();

        TeamJpaEntity entity = TeamJpaEntity.toEntity(team, section);

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("JpaTeamRepository 의 파생 쿼리 프로퍼티 경로가 연관 엔티티 식별자로 해석된다")
    void resolvesDerivedQueryProperties() {
        assertThat(PropertyPath.from("sectionId", TeamJpaEntity.class).toDotPath())
                .isEqualTo("section.id");
    }
}
