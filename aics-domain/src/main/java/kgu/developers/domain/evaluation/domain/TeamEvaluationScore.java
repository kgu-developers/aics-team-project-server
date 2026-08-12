package kgu.developers.domain.evaluation.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TeamEvaluationScore {
    private final Long id;
    private final Long teamEvaluationId;
    private final Long criterionId;
    private int score;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private TeamEvaluationScore(
            Long id,
            Long teamEvaluationId,
            Long criterionId,
            int score,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validatePositiveId(id, "평가 점수 id");
        validateRequiredPositive(teamEvaluationId, "팀간 발표평가 id");
        validateRequiredPositive(criterionId, "평가 항목 id");
        validateScore(score);

        this.id = id;
        this.teamEvaluationId = teamEvaluationId;
        this.criterionId = criterionId;
        this.score = score;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static TeamEvaluationScore create(Long teamEvaluationId, Long criterionId, int score, int maxScore) {
        validateScoreWithinMaximum(score, maxScore);
        return new TeamEvaluationScore(null, teamEvaluationId, criterionId, score, null, null, null);
    }

    public static TeamEvaluationScore restore(
            Long id,
            Long teamEvaluationId,
            Long criterionId,
            int score,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateRequiredPositive(id, "평가 점수 id");
        return new TeamEvaluationScore(id, teamEvaluationId, criterionId, score, createdAt, updatedAt, deletedAt);
    }

    public void changeScore(int score, int maxScore) {
        validateScoreWithinMaximum(score, maxScore);
        this.score = score;
    }

    private static void validatePositiveId(Long value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }

    private static void validateRequiredPositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }

    private static void validateZeroOrPositive(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + "는 0 이상이어야 합니다.");
        }
    }

    private static void validateScore(int score) {
        validateZeroOrPositive(score, "점수");
    }

    private static void validateScoreWithinMaximum(int score, int maxScore) {
        validateScore(score);
        if (maxScore <= 0) {
            throw new IllegalArgumentException("최대 점수는 양수여야 합니다.");
        }
        if (score > maxScore) {
            throw new IllegalArgumentException("점수는 최대 점수를 초과할 수 없습니다.");
        }
    }
}
