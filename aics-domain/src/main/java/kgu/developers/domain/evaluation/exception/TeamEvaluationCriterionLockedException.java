package kgu.developers.domain.evaluation.exception;

import kgu.developers.common.exception.CustomException;

public class TeamEvaluationCriterionLockedException extends CustomException {
    public TeamEvaluationCriterionLockedException() {
        super(TeamEvaluationExceptionCode.TEAM_EVALUATION_CRITERION_LOCKED);
    }
}
