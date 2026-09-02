package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.INVALID_MILESTONE_REQUEST;

import kgu.developers.common.exception.CustomException;

public class InvalidMilestoneRequestException extends CustomException {
    public InvalidMilestoneRequestException(Throwable cause) {
        super(INVALID_MILESTONE_REQUEST, cause);
    }
}
