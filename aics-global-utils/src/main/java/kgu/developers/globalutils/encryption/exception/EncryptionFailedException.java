package kgu.developers.globalutils.encryption.exception;

import static kgu.developers.globalutils.encryption.exception.EncryptionExceptionCode.ENCRYPTION_FAILED;

import kgu.developers.common.exception.CustomException;

public class EncryptionFailedException extends CustomException {
	public EncryptionFailedException(Throwable cause) {
		super(ENCRYPTION_FAILED, cause);
	}
}
