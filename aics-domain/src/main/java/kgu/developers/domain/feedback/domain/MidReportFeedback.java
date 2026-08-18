package kgu.developers.domain.feedback.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MidReportFeedback {
    private static final int AUTHOR_ID_MAX_LENGTH = 16;
    private static final int FEEDBACK_TEXT_MAX_LENGTH = 2000;

    private final Long id;
    private final Long submissionId;
    private final String authorId;
    private final String onsiteFeedbackSummary;
    private final String professorAdditionalFeedback;
    private final String revisionNote;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;

    private MidReportFeedback(
            Long id,
            Long submissionId,
            String authorId,
            String onsiteFeedbackSummary,
            String professorAdditionalFeedback,
            String revisionNote,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateOptionalPositive(id, "중간보고서 피드백 id");
        validateRequiredPositive(submissionId, "제출물 id");
        String normalizedAuthorId = normalizeAuthorId(authorId);
        String normalizedOnsiteFeedbackSummary = normalizeRequiredText(
                onsiteFeedbackSummary,
                "현장 피드백 요약",
                FEEDBACK_TEXT_MAX_LENGTH
        );
        String normalizedProfessorAdditionalFeedback = normalizeOptionalText(
                professorAdditionalFeedback,
                "교수 추가 피드백",
                FEEDBACK_TEXT_MAX_LENGTH
        );
        String normalizedRevisionNote = normalizeOptionalText(revisionNote, "수정 안내", FEEDBACK_TEXT_MAX_LENGTH);

        this.id = id;
        this.submissionId = submissionId;
        this.authorId = normalizedAuthorId;
        this.onsiteFeedbackSummary = normalizedOnsiteFeedbackSummary;
        this.professorAdditionalFeedback = normalizedProfessorAdditionalFeedback;
        this.revisionNote = normalizedRevisionNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static MidReportFeedback create(
            Long submissionId,
            String authorId,
            String onsiteFeedbackSummary,
            String professorAdditionalFeedback,
            String revisionNote
    ) {
        return new MidReportFeedback(
                null,
                submissionId,
                authorId,
                onsiteFeedbackSummary,
                professorAdditionalFeedback,
                revisionNote,
                null,
                null,
                null
        );
    }

    public static MidReportFeedback restore(
            Long id,
            Long submissionId,
            String authorId,
            String onsiteFeedbackSummary,
            String professorAdditionalFeedback,
            String revisionNote,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        validateRequiredPositive(id, "중간보고서 피드백 id");
        return new MidReportFeedback(
                id,
                submissionId,
                authorId,
                onsiteFeedbackSummary,
                professorAdditionalFeedback,
                revisionNote,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    private static String normalizeAuthorId(String authorId) {
        if (authorId == null || authorId.isBlank()) {
            throw new IllegalArgumentException("작성자 학번은 비어 있을 수 없습니다.");
        }
        String normalizedAuthorId = authorId.trim();
        if (normalizedAuthorId.length() > AUTHOR_ID_MAX_LENGTH) {
            throw new IllegalArgumentException("작성자 학번은 16자를 초과할 수 없습니다.");
        }
        return normalizedAuthorId;
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
