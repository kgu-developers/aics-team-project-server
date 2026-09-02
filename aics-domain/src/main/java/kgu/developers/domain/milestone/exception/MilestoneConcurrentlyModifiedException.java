package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_CONCURRENTLY_MODIFIED;

import kgu.developers.common.exception.CustomException;

public class MilestoneConcurrentlyModifiedException extends CustomException {

    public MilestoneConcurrentlyModifiedException() {
        super(MILESTONE_CONCURRENTLY_MODIFIED);
    }
}
