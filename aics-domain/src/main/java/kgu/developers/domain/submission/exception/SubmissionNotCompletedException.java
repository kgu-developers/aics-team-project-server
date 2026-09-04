package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_NOT_COMPLETED;

public class SubmissionNotCompletedException extends CustomException {
    public SubmissionNotCompletedException() {
        super(SUBMISSION_NOT_COMPLETED);
    }
}
