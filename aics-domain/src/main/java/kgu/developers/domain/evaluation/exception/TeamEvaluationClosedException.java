package kgu.developers.domain.evaluation.exception;

import static kgu.developers.domain.evaluation.exception.TeamEvaluationExceptionCode.TEAM_EVALUATION_CLOSED;

import kgu.developers.common.exception.CustomException;

public class TeamEvaluationClosedException extends CustomException {
    public TeamEvaluationClosedException() {
        super(TEAM_EVALUATION_CLOSED);
    }
}
