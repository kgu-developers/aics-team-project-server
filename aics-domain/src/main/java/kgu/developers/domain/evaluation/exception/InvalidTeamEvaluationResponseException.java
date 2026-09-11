package kgu.developers.domain.evaluation.exception;

import static kgu.developers.domain.evaluation.exception.TeamEvaluationExceptionCode.INVALID_TEAM_EVALUATION_RESPONSE;

import kgu.developers.common.exception.CustomException;

public class InvalidTeamEvaluationResponseException extends CustomException {
    public InvalidTeamEvaluationResponseException() {
        super(INVALID_TEAM_EVALUATION_RESPONSE);
    }
}
