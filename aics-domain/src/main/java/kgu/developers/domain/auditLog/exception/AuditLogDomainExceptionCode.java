package kgu.developers.domain.auditLog.exception;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.http.HttpStatus;

import kgu.developers.common.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuditLogDomainExceptionCode implements ExceptionCode {
	AUDIT_LOG_NOT_FOUND(NOT_FOUND, "감사 로그를 찾을 수 없습니다."),
	AUDIT_LOG_METADATA_INVALID(INTERNAL_SERVER_ERROR, "감사 로그 메타데이터를 변환하지 못했습니다."),
	;

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return this.name();
	}
}
