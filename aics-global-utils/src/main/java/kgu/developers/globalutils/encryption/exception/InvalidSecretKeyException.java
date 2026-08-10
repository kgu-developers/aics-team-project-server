package kgu.developers.globalutils.encryption.exception;

import static kgu.developers.globalutils.encryption.exception.EncryptionExceptionCode.INVALID_SECRET_KEY;

import kgu.developers.common.exception.CustomException;

public class InvalidSecretKeyException extends CustomException {
	public InvalidSecretKeyException() {
		super(INVALID_SECRET_KEY);
	}
}
