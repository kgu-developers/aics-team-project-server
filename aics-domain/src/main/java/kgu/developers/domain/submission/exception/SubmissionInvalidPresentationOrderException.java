package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_INVALID_PRESENTATION_ORDER;

public class SubmissionInvalidPresentationOrderException extends CustomException {
    public SubmissionInvalidPresentationOrderException() {
        super(SUBMISSION_INVALID_PRESENTATION_ORDER);
    }
}
