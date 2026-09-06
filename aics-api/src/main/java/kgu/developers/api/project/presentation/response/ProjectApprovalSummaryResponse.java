package kgu.developers.api.project.presentation.response;

import lombok.Builder;

@Builder
public record ProjectApprovalSummaryResponse(
    int approvedCount,
    int totalCount,
    String progress
) {
    public static ProjectApprovalSummaryResponse of(int approvedCount, int totalCount) {
        return ProjectApprovalSummaryResponse.builder()
            .approvedCount(approvedCount)
            .totalCount(totalCount)
            .progress(approvedCount + "/" + totalCount)
            .build();
    }
}
