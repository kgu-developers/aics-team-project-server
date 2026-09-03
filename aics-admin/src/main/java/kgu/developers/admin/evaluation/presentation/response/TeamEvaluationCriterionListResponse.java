package kgu.developers.admin.evaluation.presentation.response;

import java.util.List;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;

public record TeamEvaluationCriterionListResponse(
    List<TeamEvaluationCriterionResponse> contents
) {
  public static TeamEvaluationCriterionListResponse from(
      List<TeamEvaluationCriterion> criteria) {
    return new TeamEvaluationCriterionListResponse(
        criteria.stream().map(TeamEvaluationCriterionResponse::from).toList());
  }
}
