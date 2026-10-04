package kgu.developers.admin.midreport.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MidReportFeedbackCompletionAdminRequest(
    @Schema(description = "조회한 중간보고서의 현재 버전", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "중간보고서 버전은 필수입니다.")
    @PositiveOrZero(message = "중간보고서 버전은 0 이상이어야 합니다.")
    Long version
) {
}
