package kgu.developers.domain.importBatch.exception;

import static kgu.developers.domain.importBatch.exception.ImportBatchDomainExceptionCode.IMPORT_BATCH_ALREADY_APPLIED;

import kgu.developers.common.exception.CustomException;

public class ImportBatchAlreadyAppliedException extends CustomException {
	public ImportBatchAlreadyAppliedException() {
		super(IMPORT_BATCH_ALREADY_APPLIED);
	}
}
