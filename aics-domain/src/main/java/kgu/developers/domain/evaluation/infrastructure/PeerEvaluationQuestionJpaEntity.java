package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestion;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "peer_evaluation_question",
        indexes = {
                @Index(name = "idx_peer_evaluation_question_form", columnList = "form_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationQuestionJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false, length = 16)
    @Enumerated(STRING)
    private PeerEvaluationQuestionType type;

    @Column(name = "max_score", precision = 10, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public PeerEvaluationQuestion toDomain() {
        return PeerEvaluationQuestion.restore(id, formId, text, type, maxScore, displayOrder, getCreatedAt(), getUpdatedAt(), getDeletedAt());
    }

    public static PeerEvaluationQuestionJpaEntity toEntity(PeerEvaluationQuestion question) {
        PeerEvaluationQuestionJpaEntity entity = PeerEvaluationQuestionJpaEntity.builder()
                .id(question.getId())
                .formId(question.getFormId())
                .text(question.getText())
                .type(question.getType())
                .maxScore(question.getMaxScore())
                .displayOrder(question.getDisplayOrder())
                .build();
        entity.createdAt = question.getCreatedAt();
        entity.setDeletedAt(question.getDeletedAt());
        return entity;
    }
}
