package kgu.developers.domain.evaluation.domain;

import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requirePositiveId;
import static kgu.developers.domain.evaluation.domain.EvaluationDomainValidator.requireTrimmedText;
import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = PRIVATE)
public class PeerEvaluationQuestion {
    private Long id;
    private Long formId;
    private String text;
    private PeerEvaluationQuestionType type;
    private BigDecimal maxScore;
    private int displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private PeerEvaluationQuestion(
            Long id,
            Long formId,
            String text,
            PeerEvaluationQuestionType type,
            BigDecimal maxScore,
            int displayOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(formId, "상호평가 양식 식별자는 양수여야 합니다.");
        String normalizedText = requireTrimmedText(text, 500, "질문 내용은 필수입니다.", "질문 내용은 500자를 넘을 수 없습니다.");
        validateRequired(type, "질문 유형은 필수입니다.");
        if (displayOrder < 0) {
            throw new IllegalArgumentException("질문 표시 순서는 0 이상이어야 합니다.");
        }
        validateScoreRule(type, maxScore);

        this.id = id;
        this.formId = formId;
        this.text = normalizedText;
        this.type = type;
        this.maxScore = maxScore;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static PeerEvaluationQuestion createScale(Long formId, String text, BigDecimal maxScore, int displayOrder) {
        return new PeerEvaluationQuestion(null, formId, text, PeerEvaluationQuestionType.SCALE, maxScore, displayOrder, null, null, null);
    }

    public static PeerEvaluationQuestion createText(Long formId, String text, int displayOrder) {
        return new PeerEvaluationQuestion(null, formId, text, PeerEvaluationQuestionType.TEXT, null, displayOrder, null, null, null);
    }

    public static PeerEvaluationQuestion restore(
            Long id,
            Long formId,
            String text,
            PeerEvaluationQuestionType type,
            BigDecimal maxScore,
            int displayOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(id, "상호평가 질문 식별자는 양수여야 합니다.");
        return new PeerEvaluationQuestion(id, formId, text, type, maxScore, displayOrder, createdAt, updatedAt, deletedAt);
    }

    private static void validateScoreRule(PeerEvaluationQuestionType type, BigDecimal maxScore) {
        if (type == PeerEvaluationQuestionType.SCALE && (maxScore == null || maxScore.signum() <= 0)) {
            throw new IllegalArgumentException("점수형 질문의 최대 점수는 양수여야 합니다.");
        }
        if (type == PeerEvaluationQuestionType.TEXT && maxScore != null) {
            throw new IllegalArgumentException("서술형 질문은 최대 점수를 가질 수 없습니다.");
        }
    }

    private static void validateRequired(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
