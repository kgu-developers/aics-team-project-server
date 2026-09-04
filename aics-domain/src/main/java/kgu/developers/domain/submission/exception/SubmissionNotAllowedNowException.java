package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_NOT_ALLOWED_NOW;

public class SubmissionNotAllowedNowException extends CustomException {
    public SubmissionNotAllowedNowException() {
        super(SUBMISSION_NOT_ALLOWED_NOW);
    }
}
