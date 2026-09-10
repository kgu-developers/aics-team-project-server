package kgu.developers.api.evaluation.presentation.response;

import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;

public record TeamEvaluationCriterionResponse(
        Long id,
        String title,
        int maxScore,
        int displayOrder
) {
    public static TeamEvaluationCriterionResponse from(TeamEvaluationCriterion criterion) {
        return new TeamEvaluationCriterionResponse(
                criterion.getId(),
                criterion.getTitle(),
                criterion.getMaxScore(),
                criterion.getDisplayOrder()
        );
    }
}
