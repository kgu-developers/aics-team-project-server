package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_MILESTONE_TYPE_MISMATCH;

public class SubmissionMilestoneTypeMismatchException extends CustomException {
    public SubmissionMilestoneTypeMismatchException() {
        super(SUBMISSION_MILESTONE_TYPE_MISMATCH);
    }
}
