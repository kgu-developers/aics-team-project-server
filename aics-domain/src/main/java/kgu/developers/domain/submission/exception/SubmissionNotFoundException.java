package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_NOT_FOUND;

public class SubmissionNotFoundException extends CustomException {
    public SubmissionNotFoundException() {
        super(SUBMISSION_NOT_FOUND);
    }
}
