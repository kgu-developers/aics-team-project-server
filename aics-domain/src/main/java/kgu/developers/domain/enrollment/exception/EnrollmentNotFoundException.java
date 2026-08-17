package kgu.developers.domain.enrollment.exception;

import static kgu.developers.domain.enrollment.exception.EnrollmentDomainExceptionCode.ENROLLMENT_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class EnrollmentNotFoundException extends CustomException {
    public EnrollmentNotFoundException() {
        super(ENROLLMENT_NOT_FOUND);
    }
}
