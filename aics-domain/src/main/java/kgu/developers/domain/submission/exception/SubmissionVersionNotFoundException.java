package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_VERSION_NOT_FOUND;

public class SubmissionVersionNotFoundException extends CustomException {
    public SubmissionVersionNotFoundException() {
        super(SUBMISSION_VERSION_NOT_FOUND);
    }
}
