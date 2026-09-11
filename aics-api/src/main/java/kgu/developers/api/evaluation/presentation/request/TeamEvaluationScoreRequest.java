package kgu.developers.api.evaluation.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record TeamEvaluationScoreRequest(
        @NotNull @Positive Long criterionId,
        @NotNull @PositiveOrZero Integer score
) {
}
