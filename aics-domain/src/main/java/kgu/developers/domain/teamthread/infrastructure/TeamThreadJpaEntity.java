package kgu.developers.domain.teamthread.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.teamthread.domain.TeamThread;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"team_thread\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamThreadJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false, unique = true)
    private Long teamId;

    public TeamThread toDomain() {
        return TeamThread.builder()
            .id(this.id)
            .teamId(this.teamId)
            .createdAt(this.getCreatedAt())
            .build();
    }

    public static TeamThreadJpaEntity toEntity(TeamThread teamThread) {
        return TeamThreadJpaEntity.builder()
            .id(teamThread.getId())
            .teamId(teamThread.getTeamId())
            .build();
    }
}
