package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_INVALID_ARTIFACT_TYPE;

public class SubmissionInvalidArtifactTypeException extends CustomException {
    public SubmissionInvalidArtifactTypeException() {
        super(SUBMISSION_INVALID_ARTIFACT_TYPE);
    }
}
