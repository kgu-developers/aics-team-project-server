package kgu.developers.api.evaluation.presentation.response;

import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;

public record TeamEvaluationScoreResponse(
        Long criterionId,
        int score
) {
    public static TeamEvaluationScoreResponse from(TeamEvaluationScore score) {
        return new TeamEvaluationScoreResponse(score.getCriterionId(), score.getScore());
    }
}
