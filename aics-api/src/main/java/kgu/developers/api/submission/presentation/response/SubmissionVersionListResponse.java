package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionVersion;

@Builder
public record SubmissionVersionListResponse(

        @Schema(description = "버전 목록(최신순)", requiredMode = REQUIRED)
        List<SubmissionVersionSummaryResponse> contents
) {

    public static SubmissionVersionListResponse from(List<SubmissionVersion> versions) {
        return SubmissionVersionListResponse.builder()
                .contents(versions.stream().map(SubmissionVersionSummaryResponse::from).toList())
                .build();
    }
}
