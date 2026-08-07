package kgu.developers.globalutils.encryption.exception;

import static kgu.developers.globalutils.encryption.exception.EncryptionExceptionCode.DECRYPTION_FAILED;

import kgu.developers.common.exception.CustomException;

public class DecryptionFailedException extends CustomException {
	public DecryptionFailedException(Throwable cause) {
		super(DECRYPTION_FAILED, cause);
	}
}
