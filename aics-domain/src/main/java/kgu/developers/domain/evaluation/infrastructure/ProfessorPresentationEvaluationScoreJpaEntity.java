package kgu.developers.domain.evaluation.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScore;
import kgu.developers.common.domain.BaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "professor_presentation_evaluation_score", uniqueConstraints = @UniqueConstraint(
        name = "uk_professor_presentation_evaluation_score_evaluation_criterion",
        columnNames = {"evaluation_id", "criterion_id"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ProfessorPresentationEvaluationScoreJpaEntity extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;
    @Column(name = "criterion_id", nullable = false)
    private Long criterionId;
    @Column(nullable = false)
    private int score;

    public ProfessorPresentationEvaluationScore toDomain() {
        return new ProfessorPresentationEvaluationScore(id, evaluationId, criterionId, score);
    }

    public static ProfessorPresentationEvaluationScoreJpaEntity fromDomain(
            ProfessorPresentationEvaluationScore score) {
        return ProfessorPresentationEvaluationScoreJpaEntity.builder()
                .id(score.id())
                .evaluationId(score.evaluationId())
                .criterionId(score.criterionId())
                .score(score.score())
                .build();
    }
}
