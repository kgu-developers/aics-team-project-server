package kgu.developers.domain.section.exception;

import static kgu.developers.domain.section.exception.SectionDomainExceptionCode.INVALID_CONTACT_VISIBLE_PERIOD;

import kgu.developers.common.exception.CustomException;

public class InvalidContactVisiblePeriodException extends CustomException {
    public InvalidContactVisiblePeriodException() {
        super(INVALID_CONTACT_VISIBLE_PERIOD);
    }
}
