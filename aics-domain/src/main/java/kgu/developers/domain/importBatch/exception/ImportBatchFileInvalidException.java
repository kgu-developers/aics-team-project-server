package kgu.developers.domain.importBatch.exception;

import static kgu.developers.domain.importBatch.exception.ImportBatchDomainExceptionCode.IMPORT_BATCH_FILE_INVALID;

import kgu.developers.common.exception.CustomException;

public class ImportBatchFileInvalidException extends CustomException {
	public ImportBatchFileInvalidException(Throwable cause) {
		super(IMPORT_BATCH_FILE_INVALID, cause);
	}

	public ImportBatchFileInvalidException(String reason) {
		super(IMPORT_BATCH_FILE_INVALID, new IllegalArgumentException(reason));
	}
}
