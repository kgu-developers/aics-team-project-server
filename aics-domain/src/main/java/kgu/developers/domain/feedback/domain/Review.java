package kgu.developers.domain.feedback.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Review {
    private static final int REVIEWER_ID_MAX_LENGTH = 16;
    private static final int COMMENT_MAX_LENGTH = 2000;

    private final Long id;
    private final Long versionId;
    private final String reviewerId;
    private final ReviewResultStatus resultStatus;
    private final String comment;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private Review(
            Long id,
            Long versionId,
            String reviewerId,
            ReviewResultStatus resultStatus,
            String comment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateOptionalPositive(id, "리뷰 id");
        validateRequiredPositive(versionId, "버전 id");
        String normalizedReviewerId = normalizeReviewerId(reviewerId);
        validateResultStatus(resultStatus);
        String normalizedComment = normalizeOptionalText(comment, "리뷰 의견", COMMENT_MAX_LENGTH);

        this.id = id;
        this.versionId = versionId;
        this.reviewerId = normalizedReviewerId;
        this.resultStatus = resultStatus;
        this.comment = normalizedComment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Review create(
            Long versionId,
            String reviewerId,
            ReviewResultStatus resultStatus,
            String comment
    ) {
        return new Review(null, versionId, reviewerId, resultStatus, comment, null, null, null);
    }

    public static Review restore(
            Long id,
            Long versionId,
            String reviewerId,
            ReviewResultStatus resultStatus,
            String comment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateRequiredPositive(id, "리뷰 id");
        return new Review(id, versionId, reviewerId, resultStatus, comment, createdAt, updatedAt, deletedAt);
    }

    private static String normalizeReviewerId(String reviewerId) {
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new IllegalArgumentException("리뷰어 학번은 비어 있을 수 없습니다.");
        }
        String normalizedReviewerId = reviewerId.trim();
        if (normalizedReviewerId.length() > REVIEWER_ID_MAX_LENGTH) {
            throw new IllegalArgumentException("리뷰어 학번은 16자를 초과할 수 없습니다.");
        }
        return normalizedReviewerId;
    }

    private static void validateResultStatus(ReviewResultStatus resultStatus) {
        if (resultStatus == null) {
            throw new IllegalArgumentException("피드백 결과 상태는 비어 있을 수 없습니다.");
        }
    }

    private static String normalizeOptionalText(String value, String name, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + "은 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    private static void validateOptionalPositive(Long value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }

    private static void validateRequiredPositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }
}
