package kgu.developers.api.submission.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SubmissionMemberConfirmationRequest(
        @Schema(description = "최종보고서 내용을 확인했는지", example = "true", requiredMode = REQUIRED)
        @NotNull
        Boolean confirmedFinalReport,

        @Schema(description = "첨부 아티팩트를 확인했는지", example = "true", requiredMode = REQUIRED)
        @NotNull
        Boolean confirmedArtifacts,

        @Schema(description = "한 줄 소감", example = "이번 학기 정말 고생 많았습니다.")
        String oneLineReview
) {
}
