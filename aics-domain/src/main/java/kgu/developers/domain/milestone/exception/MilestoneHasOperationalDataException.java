package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_HAS_OPERATIONAL_DATA;

import kgu.developers.common.exception.CustomException;

public class MilestoneHasOperationalDataException extends CustomException {
    public MilestoneHasOperationalDataException() {
        super(MILESTONE_HAS_OPERATIONAL_DATA);
    }
}
