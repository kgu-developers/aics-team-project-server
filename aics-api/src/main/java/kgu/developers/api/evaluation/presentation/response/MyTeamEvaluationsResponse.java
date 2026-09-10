package kgu.developers.api.evaluation.presentation.response;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.evaluation.presentation.TeamEvaluationWindowState;

public record MyTeamEvaluationsResponse(
        Long milestoneId,
        TeamEvaluationWindowState windowState,
        LocalDateTime evaluationOpensAt,
        LocalDateTime evaluationClosesAt,
        List<TeamEvaluationCriterionResponse> criteria,
        List<TeamEvaluationResponse> evaluations
) {
}
