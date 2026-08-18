package kgu.developers.domain.user.exception;

import static kgu.developers.domain.user.exception.UserDomainExceptionCode.DUPLICATE_STUDENT_NUMBER;

import kgu.developers.common.exception.CustomException;

public class DuplicateStudentNumberException extends CustomException {
    public DuplicateStudentNumberException() {
        super(DUPLICATE_STUDENT_NUMBER);
    }
}
