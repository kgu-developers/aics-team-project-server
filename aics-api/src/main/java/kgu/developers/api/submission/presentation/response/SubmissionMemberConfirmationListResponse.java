package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.domain.SubmissionMemberConfirmation;

@Builder
public record SubmissionMemberConfirmationListResponse(

        @Schema(description = "팀원별 확인 현황", requiredMode = REQUIRED)
        List<SubmissionMemberConfirmationResponse> contents
) {

    public static SubmissionMemberConfirmationListResponse from(List<SubmissionMemberConfirmation> confirmations) {
        return SubmissionMemberConfirmationListResponse.builder()
                .contents(confirmations.stream().map(SubmissionMemberConfirmationResponse::from).toList())
                .build();
    }
}
