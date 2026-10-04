package kgu.developers.domain.evaluation.exception;

import kgu.developers.common.exception.CustomException;

public class TeamEvaluationCriterionNotFoundException extends CustomException {
    public TeamEvaluationCriterionNotFoundException() {
        super(TeamEvaluationExceptionCode.TEAM_EVALUATION_CRITERION_NOT_FOUND);
    }
}
