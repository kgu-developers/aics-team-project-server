package kgu.developers.api.evaluation.presentation.response;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;

public record TeamEvaluationResponse(
        Long id,
        Long teamId,
        LocalDateTime submittedAt,
        List<TeamEvaluationScoreResponse> scores
) {
    public static TeamEvaluationResponse of(
            TeamEvaluation evaluation,
            List<TeamEvaluationScore> scores
    ) {
        return new TeamEvaluationResponse(
                evaluation.getId(),
                evaluation.getRateeTeamId(),
                evaluation.getSubmittedAt(),
                scores.stream().map(TeamEvaluationScoreResponse::from).toList()
        );
    }
}
