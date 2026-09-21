package kgu.developers.domain.evaluation.exception;

import static kgu.developers.domain.evaluation.exception.PeerEvaluationExceptionCode.PEER_EVALUATION_FORM_ALREADY_EXISTS;

import kgu.developers.common.exception.CustomException;

public class PeerEvaluationFormAlreadyExistsException extends CustomException {
    public PeerEvaluationFormAlreadyExistsException() {
        super(PEER_EVALUATION_FORM_ALREADY_EXISTS);
    }
}
