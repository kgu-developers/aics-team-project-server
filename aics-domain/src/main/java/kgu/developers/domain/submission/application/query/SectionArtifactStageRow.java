package kgu.developers.domain.submission.application.query;

import java.time.LocalDateTime;

import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.submission.domain.SubmissionStatus;

public record SectionArtifactStageRow(
        MilestoneType type,
        LocalDateTime dueAt,
        SubmissionStatus status,
        LocalDateTime firstSubmittedAt,
        LocalDateTime lastSubmittedAt,
        Integer latestVersion,
        Boolean firstSubmissionLate,
        boolean overdueMissing,
        int fileCount,
        int imageCount,
        long totalFileSize
) {
}
