package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_ARTIFACT_COUNT_MISMATCH;

public class SubmissionArtifactCountMismatchException extends CustomException {
    public SubmissionArtifactCountMismatchException() {
        super(SUBMISSION_ARTIFACT_COUNT_MISMATCH);
    }
}
