package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_ACCESS_DENIED;

public class SubmissionAccessDeniedException extends CustomException {
    public SubmissionAccessDeniedException() {
        super(SUBMISSION_ACCESS_DENIED);
    }
}
