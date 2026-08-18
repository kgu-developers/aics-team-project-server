package kgu.developers.domain.section.exception;

import static kgu.developers.domain.section.exception.SectionDomainExceptionCode.INVALID_CAPACITY;

import kgu.developers.common.exception.CustomException;

public class InvalidCapacityException extends CustomException {
    public InvalidCapacityException() {
        super(INVALID_CAPACITY);
    }
}
