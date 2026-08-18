package kgu.developers.domain.user.exception;

import static kgu.developers.domain.user.exception.UserDomainExceptionCode.INVALID_CREDENTIALS;

import kgu.developers.common.exception.CustomException;

public class InvalidCredentialsException extends CustomException {
    public InvalidCredentialsException() {
        super(INVALID_CREDENTIALS);
    }
}
