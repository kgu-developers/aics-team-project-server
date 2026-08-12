package kgu.developers.domain.evaluation.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "team_evaluation_score",
        indexes = {
                @Index(name = "idx_team_evaluation_score_evaluation", columnList = "team_evaluation_id"),
                @Index(name = "idx_team_evaluation_score_criterion", columnList = "criterion_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamEvaluationScoreJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "team_evaluation_id", nullable = false)
    private Long teamEvaluationId;

    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;

    @Column(nullable = false)
    private int score;

    public TeamEvaluationScore toDomain() {
        return TeamEvaluationScore.restore(
                id,
                teamEvaluationId,
                criterionId,
                score,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt()
        );
    }

    public static TeamEvaluationScoreJpaEntity toEntity(TeamEvaluationScore score) {
        TeamEvaluationScoreJpaEntity entity = TeamEvaluationScoreJpaEntity.builder()
                .id(score.getId())
                .teamEvaluationId(score.getTeamEvaluationId())
                .criterionId(score.getCriterionId())
                .score(score.getScore())
                .build();
        entity.createdAt = score.getCreatedAt();
        entity.updatedAt = score.getUpdatedAt();
        entity.setDeletedAt(score.getDeletedAt());
        return entity;
    }
}
