package kgu.developers.api.midreport.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record MidReportBlockCompletionRequest(
    @Schema(description = "조회 응답에서 받은 중간보고서 버전", example = "3", requiredMode = REQUIRED)
    @NotNull
    @PositiveOrZero
    Long version
) {
}
