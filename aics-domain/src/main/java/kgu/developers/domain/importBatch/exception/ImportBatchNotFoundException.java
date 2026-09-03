package kgu.developers.domain.importBatch.exception;

import static kgu.developers.domain.importBatch.exception.ImportBatchDomainExceptionCode.IMPORT_BATCH_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class ImportBatchNotFoundException extends CustomException {
	public ImportBatchNotFoundException() {
		super(IMPORT_BATCH_NOT_FOUND);
	}
}
