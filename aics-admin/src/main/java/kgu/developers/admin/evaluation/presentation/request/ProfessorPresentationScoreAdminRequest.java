package kgu.developers.admin.evaluation.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ProfessorPresentationScoreAdminRequest(
    @Schema(description = "평가 항목 식별자", example = "1")
    @NotNull @Positive Long criterionId,
    @Schema(description = "점수", example = "25")
    @NotNull @PositiveOrZero Integer score
) {
}
