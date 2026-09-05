package kgu.developers.admin.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record SubmissionAdminListResponse(

        @Schema(description = "팀별 제출 현황 목록", requiredMode = REQUIRED)
        List<SubmissionAdminResponse> contents
) {

    public static SubmissionAdminListResponse from(List<SubmissionAdminResponse> contents) {
        return SubmissionAdminListResponse.builder()
                .contents(contents)
                .build();
    }
}
