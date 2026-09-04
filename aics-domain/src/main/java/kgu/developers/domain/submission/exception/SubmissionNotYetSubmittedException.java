package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_NOT_YET_SUBMITTED;

public class SubmissionNotYetSubmittedException extends CustomException {
    public SubmissionNotYetSubmittedException() {
        super(SUBMISSION_NOT_YET_SUBMITTED);
    }
}
