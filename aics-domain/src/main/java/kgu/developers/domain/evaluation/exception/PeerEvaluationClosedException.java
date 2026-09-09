package kgu.developers.domain.evaluation.exception;

import static kgu.developers.domain.evaluation.exception.PeerEvaluationExceptionCode.PEER_EVALUATION_CLOSED;

import kgu.developers.common.exception.CustomException;

public class PeerEvaluationClosedException extends CustomException {
    public PeerEvaluationClosedException() {
        super(PEER_EVALUATION_CLOSED);
    }
}
