package kgu.developers.domain.importBatch.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImportBatchDomainExceptionCode implements ExceptionCode {
	IMPORT_BATCH_PAYLOAD_INVALID(INTERNAL_SERVER_ERROR, "업로드 원본 데이터를 변환하지 못했습니다."),
	IMPORT_BATCH_ALREADY_APPLIED(CONFLICT, "이미 적용된 업로드입니다."),
	IMPORT_BATCH_EXPIRED(GONE, "만료된 업로드입니다. 파일을 다시 업로드해주세요."),
	IMPORT_BATCH_HAS_INVALID_ROWS(BAD_REQUEST, "오류가 있는 행이 남아 있어 적용할 수 없습니다."),
	IMPORT_BATCH_NOT_FOUND(NOT_FOUND, "존재하지 않는 업로드입니다."),
	IMPORT_BATCH_FILE_INVALID(BAD_REQUEST, "엑셀 파일을 읽을 수 없습니다."),
	;

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
