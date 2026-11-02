package kgu.developers.domain.teammessage.domain;

import java.time.LocalDateTime;

public record DocumentFeedbackSummary(
    long feedbackCount,
    LocalDateTime lastFeedbackAt,
    LocalDateTime lastStudentMessageAt
) {
    public static final DocumentFeedbackSummary EMPTY = new DocumentFeedbackSummary(0, null, null);

    public boolean feedbackRequested(LocalDateTime submittedAt) {
        return isAfterFeedback(submittedAt) || isAfterFeedback(lastStudentMessageAt);
    }

    private boolean isAfterFeedback(LocalDateTime eventAt) {
        return eventAt != null && (lastFeedbackAt == null || eventAt.isAfter(lastFeedbackAt));
    }
}
