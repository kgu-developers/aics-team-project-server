package kgu.developers.domain.user.exception;

import static kgu.developers.domain.user.exception.UserDomainExceptionCode.DUPLICATE_EMAIL;

import kgu.developers.common.exception.CustomException;

public class DuplicateEmailException extends CustomException {
    public DuplicateEmailException() {
        super(DUPLICATE_EMAIL);
    }
}
