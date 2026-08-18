package kgu.developers.domain.importBatch.exception;

import static kgu.developers.domain.importBatch.exception.ImportBatchDomainExceptionCode.IMPORT_BATCH_HAS_INVALID_ROWS;

import kgu.developers.common.exception.CustomException;

public class ImportBatchHasInvalidRowsException extends CustomException {
	public ImportBatchHasInvalidRowsException() {
		super(IMPORT_BATCH_HAS_INVALID_ROWS);
	}
}
