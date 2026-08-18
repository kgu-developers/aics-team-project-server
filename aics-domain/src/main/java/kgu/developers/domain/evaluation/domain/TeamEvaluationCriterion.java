package kgu.developers.domain.evaluation.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TeamEvaluationCriterion {
    private static final int TITLE_MAX_LENGTH = 100;

    private final Long id;
    private final Long sectionId;
    private final String title;
    private final int maxScore;
    private final int displayOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private TeamEvaluationCriterion(
            Long id,
            Long sectionId,
            String title,
            int maxScore,
            int displayOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validatePositiveId(id, "평가 항목 id");
        validateRequiredPositive(sectionId, "분반 id");
        String normalizedTitle = normalizeTitle(title);
        validatePositive(maxScore, "최대 점수");
        validateZeroOrPositive(displayOrder, "표시 순서");

        this.id = id;
        this.sectionId = sectionId;
        this.title = normalizedTitle;
        this.maxScore = maxScore;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static TeamEvaluationCriterion create(Long sectionId, String title, int maxScore, int displayOrder) {
        return new TeamEvaluationCriterion(null, sectionId, title, maxScore, displayOrder, null, null, null);
    }

    public static TeamEvaluationCriterion restore(
            Long id,
            Long sectionId,
            String title,
            int maxScore,
            int displayOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateRequiredPositive(id, "평가 항목 id");
        return new TeamEvaluationCriterion(id, sectionId, title, maxScore, displayOrder, createdAt, updatedAt, deletedAt);
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("평가 항목 제목은 비어 있을 수 없습니다.");
        }
        String normalizedTitle = title.trim();
        if (normalizedTitle.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("평가 항목 제목은 100자를 초과할 수 없습니다.");
        }
        return normalizedTitle;
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

    private static void validatePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }

    private static void validateZeroOrPositive(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + "는 0 이상이어야 합니다.");
        }
    }
}
