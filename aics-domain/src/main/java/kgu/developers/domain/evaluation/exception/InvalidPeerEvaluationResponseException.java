package kgu.developers.domain.evaluation.exception;

import static kgu.developers.domain.evaluation.exception.PeerEvaluationExceptionCode.INVALID_PEER_EVALUATION_RESPONSE;

import kgu.developers.common.exception.CustomException;

public class InvalidPeerEvaluationResponseException extends CustomException {
    public InvalidPeerEvaluationResponseException() {
        super(INVALID_PEER_EVALUATION_RESPONSE);
    }
}
