package kgu.developers.api.evaluation.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record PeerEvaluationAnswerRequest(
    @Schema(allowableValues = {"TEAMMATE_CONTRIBUTION", "REFLECTION"})
    String kind,
    String targetUserId,
    Integer contributionPercent,
    String contributionDetail,
    String teammateAssessment,
    String comment
) {
}
