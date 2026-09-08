package kgu.developers.domain.evaluation.exception;

import static kgu.developers.domain.evaluation.exception.PeerEvaluationExceptionCode.PEER_EVALUATION_FORM_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class PeerEvaluationFormNotFoundException extends CustomException {
    public PeerEvaluationFormNotFoundException() {
        super(PEER_EVALUATION_FORM_NOT_FOUND);
    }
}
