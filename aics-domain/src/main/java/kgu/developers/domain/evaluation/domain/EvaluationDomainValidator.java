package kgu.developers.domain.evaluation.domain;

import java.math.BigDecimal;

final class EvaluationDomainValidator {
    private EvaluationDomainValidator() {
    }

    static Long requirePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
        return id;
    }

    static BigDecimal requireNumber(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static BigDecimal requireNonNegative(BigDecimal value, String message) {
        requireNumber(value, message);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static String requireTrimmedText(String value, int maxLength, String requiredMessage, String lengthMessage) {
        if (value == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }

    static String trimNullableText(String value, int maxLength, String lengthMessage) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }
}
