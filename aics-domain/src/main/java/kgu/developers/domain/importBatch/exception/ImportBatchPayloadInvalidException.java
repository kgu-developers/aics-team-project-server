package kgu.developers.domain.importBatch.exception;

import static kgu.developers.domain.importBatch.exception.ImportBatchDomainExceptionCode.IMPORT_BATCH_PAYLOAD_INVALID;

import kgu.developers.common.exception.CustomException;

public class ImportBatchPayloadInvalidException extends CustomException {
	public ImportBatchPayloadInvalidException(Throwable cause) {
		super(IMPORT_BATCH_PAYLOAD_INVALID, cause);
	}
}
