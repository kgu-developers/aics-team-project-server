package kgu.developers.domain.enrollment.exception;

import static kgu.developers.domain.enrollment.exception.EnrollmentDomainExceptionCode.DUPLICATE_ENROLLMENT;

import kgu.developers.common.exception.CustomException;

public class DuplicateEnrollmentException extends CustomException {
    public DuplicateEnrollmentException() {
        super(DUPLICATE_ENROLLMENT);
    }
}
