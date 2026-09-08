package kgu.developers.api.evaluation.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PeerEvaluationAnswerResponse(
    @Schema(allowableValues = {"TEAMMATE_CONTRIBUTION", "REFLECTION"})
    String kind,
    String targetUserId,
    Integer contributionPercent,
    String contributionDetail,
    String teammateAssessment,
    String comment
) {
    public static PeerEvaluationAnswerResponse teammate(
        String targetUserId,
        int contributionPercent,
        String contributionDetail,
        String teammateAssessment
    ) {
        return new PeerEvaluationAnswerResponse(
            "TEAMMATE_CONTRIBUTION", targetUserId, contributionPercent,
            contributionDetail, teammateAssessment, null
        );
    }

    public static PeerEvaluationAnswerResponse reflection(String comment) {
        return new PeerEvaluationAnswerResponse("REFLECTION", null, null, null, null, comment);
    }
}
