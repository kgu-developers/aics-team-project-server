package kgu.developers.domain.section.exception;

import static kgu.developers.domain.section.exception.SectionDomainExceptionCode.CONTACT_NOT_VISIBLE;

import kgu.developers.common.exception.CustomException;

public class ContactNotVisibleException extends CustomException {
    public ContactNotVisibleException() {
        super(CONTACT_NOT_VISIBLE);
    }
}
