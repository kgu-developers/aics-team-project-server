package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_WEEK_CONFLICT;

import org.hibernate.exception.ConstraintViolationException;

import kgu.developers.common.exception.CustomException;

public class DuplicateMilestoneWeekException extends CustomException {
    private static final String ACTIVE_WEEK_CONSTRAINT = "uq_milestone_active_section_week";

    public DuplicateMilestoneWeekException() {
        super(MILESTONE_WEEK_CONFLICT);
    }

    public DuplicateMilestoneWeekException(Throwable cause) {
        super(MILESTONE_WEEK_CONFLICT, cause);
    }

    public static boolean isActiveWeekConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                    && ACTIVE_WEEK_CONSTRAINT.equals(constraintViolationException.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
