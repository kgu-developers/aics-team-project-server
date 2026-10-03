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
    public boolean hasSubmissionHistory() {
        // 재제출로 제출 시각이 사라진 중간점검도 기준일 이전 반려로 제출 이력을 확인할 수 있다.
        return firstSubmittedAt != null
                || (type == MilestoneType.MID_REPORT && status == SubmissionStatus.REVISION_REQUESTED);
    }
}
