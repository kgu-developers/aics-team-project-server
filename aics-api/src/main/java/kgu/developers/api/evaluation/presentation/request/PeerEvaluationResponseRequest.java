package kgu.developers.api.evaluation.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PeerEvaluationResponseRequest(
    String selfContribution,
    String projectReviewComment,
    @NotNull List<@NotNull @Valid PeerEvaluationAnswerRequest> answers,
    boolean submit
) {
}
