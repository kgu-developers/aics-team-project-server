package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_WEEK_CONFLICT;

import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import kgu.developers.common.exception.CustomException;

public class DuplicateMilestoneWeekException extends CustomException {
    private static final String ACTIVE_WEEK_CONSTRAINT = "uq_milestone_active_section_week";

    public DuplicateMilestoneWeekException() {
        super(MILESTONE_WEEK_CONFLICT);
    }

    public DuplicateMilestoneWeekException(Throwable cause) {
        super(MILESTONE_WEEK_CONFLICT, cause);
    }

    public static RuntimeException translate(DataIntegrityViolationException exception) {
        if (isActiveWeekConstraintViolation(exception)) {
            return new DuplicateMilestoneWeekException(exception);
        }
        return exception;
    }

    public static boolean isActiveWeekConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                    && isActiveWeekConstraint(constraintViolationException.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isActiveWeekConstraint(String constraintName) {
        if (constraintName == null) {
            return false;
        }

        String normalizedName = constraintName
                .replace("\"", "")
                .toLowerCase(Locale.ROOT);
        int schemaSeparator = normalizedName.lastIndexOf('.');
        String unqualifiedName = schemaSeparator >= 0
                ? normalizedName.substring(schemaSeparator + 1)
                : normalizedName;
        return ACTIVE_WEEK_CONSTRAINT.equals(unqualifiedName);
    }
}
