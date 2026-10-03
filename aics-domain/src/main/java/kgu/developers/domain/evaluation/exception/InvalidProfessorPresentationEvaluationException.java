package kgu.developers.domain.evaluation.exception;

import kgu.developers.common.exception.CustomException;

public class InvalidProfessorPresentationEvaluationException extends CustomException {
    public InvalidProfessorPresentationEvaluationException() {
        super(TeamEvaluationExceptionCode.INVALID_PROFESSOR_PRESENTATION_EVALUATION);
    }
}
