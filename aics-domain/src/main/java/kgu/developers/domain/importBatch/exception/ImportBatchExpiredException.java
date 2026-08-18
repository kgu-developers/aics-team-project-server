package kgu.developers.domain.importBatch.exception;

import static kgu.developers.domain.importBatch.exception.ImportBatchDomainExceptionCode.IMPORT_BATCH_EXPIRED;

import kgu.developers.common.exception.CustomException;

public class ImportBatchExpiredException extends CustomException {
	public ImportBatchExpiredException() {
		super(IMPORT_BATCH_EXPIRED);
	}
}
