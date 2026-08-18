package kgu.developers.domain.user.exception;

import static kgu.developers.domain.user.exception.UserDomainExceptionCode.INVALID_TOKEN;

import kgu.developers.common.exception.CustomException;

public class InvalidTokenException extends CustomException {
    public InvalidTokenException() {
        super(INVALID_TOKEN);
    }
}
