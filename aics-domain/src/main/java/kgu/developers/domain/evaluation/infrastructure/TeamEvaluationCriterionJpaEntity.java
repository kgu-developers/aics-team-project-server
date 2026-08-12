package kgu.developers.domain.evaluation.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
        name = "team_evaluation_criterion",
        indexes = {
                @Index(name = "idx_team_evaluation_criterion_section_order", columnList = "section_id, display_order")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamEvaluationCriterionJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public TeamEvaluationCriterion toDomain() {
        return TeamEvaluationCriterion.restore(
                id,
                sectionId,
                title,
                maxScore,
                displayOrder,
                getCreatedAt(),
                getUpdatedAt(),
                getDeletedAt()
        );
    }

    public static TeamEvaluationCriterionJpaEntity toEntity(TeamEvaluationCriterion criterion) {
        TeamEvaluationCriterionJpaEntity entity = TeamEvaluationCriterionJpaEntity.builder()
                .id(criterion.getId())
                .sectionId(criterion.getSectionId())
                .title(criterion.getTitle())
                .maxScore(criterion.getMaxScore())
                .displayOrder(criterion.getDisplayOrder())
                .build();
        entity.createdAt = criterion.getCreatedAt();
        entity.updatedAt = criterion.getUpdatedAt();
        entity.setDeletedAt(criterion.getDeletedAt());
        return entity;
    }
}
