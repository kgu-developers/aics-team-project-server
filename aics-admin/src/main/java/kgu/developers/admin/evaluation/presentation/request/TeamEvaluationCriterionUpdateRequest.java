package kgu.developers.admin.evaluation.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TeamEvaluationCriterionUpdateRequest(
    @Schema(description = "평가 항목명", example = "객체지향 설계")
    @NotBlank @Size(max = 100) String title,
    @Schema(description = "항목 최대 점수", example = "30")
    @NotNull @Positive Integer maxScore,
    @Schema(description = "표시 순서", example = "0")
    @NotNull @PositiveOrZero Integer displayOrder
) {
  public TeamEvaluationCriterionUpdateRequest {
    if (title != null) {
      title = title.trim();
    }
  }
}
