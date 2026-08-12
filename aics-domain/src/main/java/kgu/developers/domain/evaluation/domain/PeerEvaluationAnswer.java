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
public class PeerEvaluationAnswer {
    private Long id;
    private Long responseId;
    private Long questionId;
    private BigDecimal score;
    private String textAnswer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private PeerEvaluationAnswer(
            Long id,
            Long responseId,
            Long questionId,
            BigDecimal score,
            String textAnswer,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(responseId, "상호평가 응답 식별자는 양수여야 합니다.");
        requirePositiveId(questionId, "상호평가 질문 식별자는 양수여야 합니다.");
        String normalizedTextAnswer = validateAnswerShape(score, textAnswer);

        this.id = id;
        this.responseId = responseId;
        this.questionId = questionId;
        this.score = score;
        this.textAnswer = normalizedTextAnswer;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static PeerEvaluationAnswer createScore(
            Long responseId,
            Long questionId,
            BigDecimal score,
            BigDecimal maxScore
    ) {
        validateScoreWithinMaximum(score, maxScore);
        return new PeerEvaluationAnswer(null, responseId, questionId, score, null, null, null, null);
    }

    public static PeerEvaluationAnswer createText(Long responseId, Long questionId, String textAnswer) {
        return new PeerEvaluationAnswer(null, responseId, questionId, null, textAnswer, null, null, null);
    }

    public static PeerEvaluationAnswer restore(
            Long id,
            Long responseId,
            Long questionId,
            BigDecimal score,
            String textAnswer,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        requirePositiveId(id, "상호평가 답변 식별자는 양수여야 합니다.");
        return new PeerEvaluationAnswer(id, responseId, questionId, score, textAnswer, createdAt, updatedAt, deletedAt);
    }

    private static String validateAnswerShape(BigDecimal score, String textAnswer) {
        boolean hasScore = score != null;
        boolean hasText = textAnswer != null && !textAnswer.isBlank();
        if (hasScore == hasText) {
            throw new IllegalArgumentException("답변은 점수 또는 서술 답변 중 정확히 하나만 가져야 합니다.");
        }
        if (score != null && score.signum() < 0) {
            throw new IllegalArgumentException("답변 점수는 0 이상이어야 합니다.");
        }
        if (!hasText) {
            return null;
        }
        return requireTrimmedText(textAnswer, 2000, "서술 답변은 필수입니다.", "서술 답변은 2000자를 넘을 수 없습니다.");
    }

    private static void validateScoreWithinMaximum(BigDecimal score, BigDecimal maxScore) {
        if (score == null) {
            throw new IllegalArgumentException("답변 점수는 필수입니다.");
        }
        if (maxScore == null || maxScore.signum() <= 0) {
            throw new IllegalArgumentException("질문의 최대 점수는 양수여야 합니다.");
        }
        if (score.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException("답변 점수는 질문의 최대 점수를 초과할 수 없습니다.");
        }
    }
}
