package kgu.developers.globalutils.encryption.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EncryptionExceptionCode implements ExceptionCode {
	INVALID_SECRET_KEY(INTERNAL_SERVER_ERROR, "file.secret-key는 UTF-8 기준 16/24/32바이트여야 합니다."),
	ENCRYPTION_FAILED(INTERNAL_SERVER_ERROR, "암호화에 실패했습니다."),
	DECRYPTION_FAILED(BAD_REQUEST, "복호화에 실패했습니다."),
	;

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
