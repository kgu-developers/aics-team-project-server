package kgu.developers.domain.evaluation.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.evaluation.domain.PeerEvaluationAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "peer_evaluation_answer",
        indexes = {
                @Index(name = "idx_peer_evaluation_answer_response", columnList = "response_id"),
                @Index(name = "idx_peer_evaluation_answer_question", columnList = "question_id")
        }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PeerEvaluationAnswerJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "text_answer", length = 2000)
    private String textAnswer;

    public PeerEvaluationAnswer toDomain() {
        return PeerEvaluationAnswer.restore(id, responseId, questionId, score, textAnswer, getCreatedAt(), getUpdatedAt(), getDeletedAt());
    }

    public static PeerEvaluationAnswerJpaEntity toEntity(PeerEvaluationAnswer answer) {
        PeerEvaluationAnswerJpaEntity entity = PeerEvaluationAnswerJpaEntity.builder()
                .id(answer.getId())
                .responseId(answer.getResponseId())
                .questionId(answer.getQuestionId())
                .score(answer.getScore())
                .textAnswer(answer.getTextAnswer())
                .build();
        entity.createdAt = answer.getCreatedAt();
        entity.setDeletedAt(answer.getDeletedAt());
        return entity;
    }
}
