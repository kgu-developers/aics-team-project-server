package kgu.developers.admin.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionVersion;

@Builder
public record SubmissionVersionAdminListResponse(

        @Schema(description = "버전 목록(최신순)", requiredMode = REQUIRED)
        List<SubmissionVersionAdminSummaryResponse> contents
) {

    public static SubmissionVersionAdminListResponse from(List<SubmissionVersion> versions) {
        return SubmissionVersionAdminListResponse.builder()
                .contents(versions.stream().map(SubmissionVersionAdminSummaryResponse::from).toList())
                .build();
    }
}
