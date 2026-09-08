package kgu.developers.api.evaluation.presentation.request;

import kgu.developers.api.evaluation.presentation.PeerEvaluationAnswerKind;

public record PeerEvaluationAnswerRequest(
    PeerEvaluationAnswerKind kind,
    String targetUserId,
    Integer contributionPercent,
    String contributionDetail,
    String teammateAssessment,
    String comment
) {
}
