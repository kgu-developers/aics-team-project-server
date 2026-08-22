package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_WEEK_CONFLICT;

import kgu.developers.common.exception.CustomException;

public class DuplicateMilestoneWeekException extends CustomException {
    public DuplicateMilestoneWeekException() {
        super(MILESTONE_WEEK_CONFLICT);
    }

    public DuplicateMilestoneWeekException(Throwable cause) {
        super(MILESTONE_WEEK_CONFLICT, cause);
    }
}
