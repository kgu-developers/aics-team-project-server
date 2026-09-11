package kgu.developers.api.evaluation.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TeamEvaluationSubmitRequest(
        @NotEmpty List<@Valid TeamEvaluationScoreRequest> scores
) {
}
