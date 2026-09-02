package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_PRESENTATION_IMAGE_OWNERSHIP_INVALID;

public class SubmissionPresentationImageOwnershipException extends CustomException {
    public SubmissionPresentationImageOwnershipException() {
        super(SUBMISSION_PRESENTATION_IMAGE_OWNERSHIP_INVALID);
    }
}
