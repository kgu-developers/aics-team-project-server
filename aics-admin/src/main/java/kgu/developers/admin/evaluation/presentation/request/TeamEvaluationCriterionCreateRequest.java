package kgu.developers.admin.evaluation.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record TeamEvaluationCriterionCreateRequest(
    @Schema(
        description = "평가 항목명(앞뒤 공백 제거 후 최대 100자)",
        example = "객체지향 설계",
        maxLength = 100,
        requiredMode = REQUIRED)
    @NotBlank
    String title,

    @Schema(description = "항목 최대 점수", example = "30", requiredMode = REQUIRED)
    @NotNull
    @Positive
    Integer maxScore,

    @Schema(description = "표시 순서", example = "0", requiredMode = REQUIRED)
    @NotNull
    @PositiveOrZero
    Integer displayOrder
) {
}
