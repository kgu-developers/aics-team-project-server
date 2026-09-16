package kgu.developers.admin.proposal.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProposalFeedbackAdminRequest(
    @Schema(description = "피드백 내용", example = "데이터 구성의 수집 방법을 구체적으로 적어 주세요.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "피드백 내용은 비어 있을 수 없습니다.")
    @Size(max = 2000, message = "피드백 내용은 2000자를 초과할 수 없습니다.")
    String message
) {
}
