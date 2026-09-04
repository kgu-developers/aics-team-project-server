package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_REQUIRED_ARTIFACT_MISMATCH;

public class SubmissionRequiredArtifactMismatchException extends CustomException {
    public SubmissionRequiredArtifactMismatchException() {
        super(SUBMISSION_REQUIRED_ARTIFACT_MISMATCH);
    }
}
