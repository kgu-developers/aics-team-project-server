package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.user.domain.User;

@Builder
public record SubmissionVersionListResponse(

        @Schema(description = "버전 목록(최신순)", requiredMode = REQUIRED)
        List<SubmissionVersionSummaryResponse> contents
) {

    public static SubmissionVersionListResponse from(
            List<SubmissionVersion> versions,
            Map<Long, List<SubmissionArtifactResponse>> artifactsByVersionId,
            Map<String, User> submittersByUserId) {
        return SubmissionVersionListResponse.builder()
                .contents(versions.stream()
                        .map(version -> SubmissionVersionSummaryResponse.from(
                                version,
                                submittersByUserId.get(version.getSubmittedBy()),
                                artifactsByVersionId.getOrDefault(version.getId(), List.of())))
                        .toList())
                .build();
    }
}
