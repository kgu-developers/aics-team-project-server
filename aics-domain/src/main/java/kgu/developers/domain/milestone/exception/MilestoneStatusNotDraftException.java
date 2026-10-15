package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_STATUS_NOT_DRAFT;

import kgu.developers.common.exception.CustomException;

public class MilestoneStatusNotDraftException extends CustomException {
    public MilestoneStatusNotDraftException() {
        super(MILESTONE_STATUS_NOT_DRAFT);
    }
}
