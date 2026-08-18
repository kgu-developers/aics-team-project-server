package kgu.developers.domain.feedback.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RequiredArtifact {
    private static final int LABEL_MAX_LENGTH = 100;
    private static final int ALLOWED_EXTENSIONS_MAX_LENGTH = 255;

    private final Long id;
    private final Long milestoneId;
    private final RequiredArtifactType type;
    private final String label;
    private final boolean required;
    private final String allowedExtensions;
    private final Integer maxFileSizeMb;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private RequiredArtifact(
            Long id,
            Long milestoneId,
            RequiredArtifactType type,
            String label,
            boolean required,
            String allowedExtensions,
            Integer maxFileSizeMb,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateOptionalPositive(id, "필수 산출물 id");
        validateRequiredPositive(milestoneId, "마일스톤 id");
        validateType(type);
        String normalizedLabel = normalizeRequiredText(label, "필수 산출물 이름", LABEL_MAX_LENGTH);
        String normalizedAllowedExtensions = normalizeOptionalText(
                allowedExtensions,
                "허용 확장자",
                ALLOWED_EXTENSIONS_MAX_LENGTH
        );
        validateOptionalPositive(maxFileSizeMb, "최대 파일 크기");

        this.id = id;
        this.milestoneId = milestoneId;
        this.type = type;
        this.label = normalizedLabel;
        this.required = required;
        this.allowedExtensions = normalizedAllowedExtensions;
        this.maxFileSizeMb = maxFileSizeMb;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static RequiredArtifact create(
            Long milestoneId,
            RequiredArtifactType type,
            String label,
            boolean required,
            String allowedExtensions,
            Integer maxFileSizeMb
    ) {
        return new RequiredArtifact(null, milestoneId, type, label, required, allowedExtensions, maxFileSizeMb, null, null, null);
    }

    public static RequiredArtifact restore(
            Long id,
            Long milestoneId,
            RequiredArtifactType type,
            String label,
            boolean required,
            String allowedExtensions,
            Integer maxFileSizeMb,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateRequiredPositive(id, "필수 산출물 id");
        return new RequiredArtifact(
                id,
                milestoneId,
                type,
                label,
                required,
                allowedExtensions,
                maxFileSizeMb,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    private static void validateType(RequiredArtifactType type) {
        if (type == null) {
            throw new IllegalArgumentException("필수 산출물 유형은 비어 있을 수 없습니다.");
        }
    }

    private static String normalizeRequiredText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "은 비어 있을 수 없습니다.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + "은 " + maxLength + "자를 초과할 수 없습니다.");
        }
        return normalized;
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
            throw new IllegalArgumentException(name + "는 " + maxLength + "자를 초과할 수 없습니다.");
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

    private static void validateOptionalPositive(Integer value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + "는 양수여야 합니다.");
        }
    }
}
