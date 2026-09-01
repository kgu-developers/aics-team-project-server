package kgu.developers.domain.auditLog.exception;

import static kgu.developers.domain.auditLog.exception.AuditLogDomainExceptionCode.AUDIT_LOG_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class AuditLogNotFoundException extends CustomException {
	public AuditLogNotFoundException() {
		super(AUDIT_LOG_NOT_FOUND);
	}
}
