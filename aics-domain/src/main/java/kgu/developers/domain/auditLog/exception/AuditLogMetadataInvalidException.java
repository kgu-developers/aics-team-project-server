package kgu.developers.domain.auditLog.exception;

import static kgu.developers.domain.auditLog.exception.AuditLogDomainExceptionCode.AUDIT_LOG_METADATA_INVALID;

import kgu.developers.common.exception.CustomException;

public class AuditLogMetadataInvalidException extends CustomException {
	public AuditLogMetadataInvalidException(Throwable cause) {
		super(AUDIT_LOG_METADATA_INVALID, cause);
	}
}
