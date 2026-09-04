package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_INVALID_SCREENS;

public class SubmissionInvalidScreensException extends CustomException {
    public SubmissionInvalidScreensException() {
        super(SUBMISSION_INVALID_SCREENS);
    }
}
