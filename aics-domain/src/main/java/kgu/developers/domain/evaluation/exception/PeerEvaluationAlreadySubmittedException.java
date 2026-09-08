package kgu.developers.domain.evaluation.exception;

import static kgu.developers.domain.evaluation.exception.PeerEvaluationExceptionCode.PEER_EVALUATION_ALREADY_SUBMITTED;

import kgu.developers.common.exception.CustomException;

public class PeerEvaluationAlreadySubmittedException extends CustomException {
    public PeerEvaluationAlreadySubmittedException() {
        super(PEER_EVALUATION_ALREADY_SUBMITTED);
    }
}
