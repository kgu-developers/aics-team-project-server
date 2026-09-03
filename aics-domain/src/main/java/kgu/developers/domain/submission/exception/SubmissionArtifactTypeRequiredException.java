package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_ARTIFACT_TYPE_REQUIRED;

public class SubmissionArtifactTypeRequiredException extends CustomException {
    public SubmissionArtifactTypeRequiredException() {
        super(SUBMISSION_ARTIFACT_TYPE_REQUIRED);
    }
}
