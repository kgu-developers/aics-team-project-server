package teamMember.infrastructure;

import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberJpaEntity;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PropertyPath;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMemberJpaEntityTest {

    private final TeamJpaEntity team = TeamJpaEntity.builder().id(100L).build();
    private final UserJpaEntity user = UserJpaEntity.builder().studentNumber("202012345").build();

    @Test
    @DisplayName("TeamMember <-> TeamMemberJpaEntity 양방향 변환 시 teamId, userId, isLeader, projectRole 모든 필드가 전달된다")
    void roundTrip() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 1, 10, 0);
        TeamMember origin = TeamMember.builder()
                .id(1L)
                .teamId(100L)
                .userId("202012345")
                .isLeader(true)
                .projectRole("백엔드 Leader")
                .createdAt(createdAt)
                .build();

        TeamMemberJpaEntity entity = TeamMemberJpaEntity.toEntity(origin, team, user);
        TeamMember restored = entity.toDomain();

        assertThat(restored.getId()).isEqualTo(origin.getId());
        assertThat(restored.getTeamId()).isEqualTo(origin.getTeamId());
        assertThat(restored.getUserId()).isEqualTo(origin.getUserId());
        assertThat(restored.isLeader()).isTrue();
        assertThat(restored.getProjectRole()).isEqualTo("백엔드 Leader");
        assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("toEntity 전환 시 deletedAt 정보가 전달된다")
    void carriesDeletedAt() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 15, 0);
        TeamMember origin = TeamMember.builder()
                .id(1L)
                .teamId(100L)
                .userId("202012345")
                .isLeader(false)
                .projectRole("프론트엔드 Member")
                .deletedAt(deletedAt)
                .build();

        TeamMemberJpaEntity entity = TeamMemberJpaEntity.toEntity(origin, team, user);

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("JpaTeamMemberRepository 의 파생 쿼리 프로퍼티 경로가 연관 엔티티 식별자로 올바르게 해석된다")
    void resolvesDerivedQueryProperties() {
        assertThat(PropertyPath.from("teamId", TeamMemberJpaEntity.class).toDotPath())
                .isEqualTo("team.id");
        assertThat(PropertyPath.from("userStudentNumber", TeamMemberJpaEntity.class).toDotPath())
                .isEqualTo("user.studentNumber");
    }
}
