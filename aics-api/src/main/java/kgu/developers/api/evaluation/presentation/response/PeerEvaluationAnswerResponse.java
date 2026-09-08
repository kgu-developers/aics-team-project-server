package kgu.developers.api.evaluation.presentation.response;

import kgu.developers.api.evaluation.presentation.PeerEvaluationAnswerKind;

public record PeerEvaluationAnswerResponse(
    PeerEvaluationAnswerKind kind,
    String targetUserId,
    Integer contributionPercent,
    String contributionDetail,
    String teammateAssessment,
    String comment
) {
    public static PeerEvaluationAnswerResponse teammate(
        String targetUserId,
        Integer contributionPercent,
        String contributionDetail,
        String teammateAssessment
    ) {
        return new PeerEvaluationAnswerResponse(
            PeerEvaluationAnswerKind.TEAMMATE_CONTRIBUTION, targetUserId, contributionPercent,
            contributionDetail, teammateAssessment, null
        );
    }

    public static PeerEvaluationAnswerResponse reflection(String comment) {
        return new PeerEvaluationAnswerResponse(PeerEvaluationAnswerKind.REFLECTION, null, null, null, null, comment);
    }
}
