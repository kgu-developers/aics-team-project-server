package kgu.developers.admin.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.user.domain.User;

@Builder
public record SubmissionVersionAdminListResponse(

        @Schema(description = "버전 목록(최신순)", requiredMode = REQUIRED)
        List<SubmissionVersionAdminSummaryResponse> contents
) {

    public static SubmissionVersionAdminListResponse from(
            List<SubmissionVersion> versions, Map<String, User> submittersByUserId) {
        return SubmissionVersionAdminListResponse.builder()
                .contents(versions.stream()
                        .map(version -> SubmissionVersionAdminSummaryResponse.from(
                                version, submittersByUserId.get(version.getSubmittedBy())))
                        .toList())
                .build();
    }
}
