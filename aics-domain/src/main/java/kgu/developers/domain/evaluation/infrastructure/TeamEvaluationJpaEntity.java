package kgu.developers.domain.evaluation.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "team_evaluation",
        indexes = {
                @Index(name = "idx_team_evaluation_milestone_rater", columnList = "milestone_id, rater_id"),
                @Index(name = "idx_team_evaluation_ratee_team", columnList = "ratee_team_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamEvaluationJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "milestone_id", nullable = false)
    private Long milestoneId;

    @Column(name = "rater_id", nullable = false, length = 16)
    private String raterId;

    @Column(name = "ratee_team_id", nullable = false)
    private Long rateeTeamId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public TeamEvaluation toDomain() {
        return TeamEvaluation.restore(
                id,
                milestoneId,
                raterId,
                rateeTeamId,
                submittedAt,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt()
        );
    }

    public static TeamEvaluationJpaEntity toEntity(TeamEvaluation evaluation) {
        TeamEvaluationJpaEntity entity = TeamEvaluationJpaEntity.builder()
                .id(evaluation.getId())
                .milestoneId(evaluation.getMilestoneId())
                .raterId(evaluation.getRaterId())
                .rateeTeamId(evaluation.getRateeTeamId())
                .submittedAt(evaluation.getSubmittedAt())
                .build();
        entity.createdAt = evaluation.getCreatedAt();
        entity.setDeletedAt(evaluation.getDeletedAt());
        return entity;
    }
}
