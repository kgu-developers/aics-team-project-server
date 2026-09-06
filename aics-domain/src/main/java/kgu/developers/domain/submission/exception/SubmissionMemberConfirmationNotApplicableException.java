package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_MEMBER_CONFIRMATION_NOT_APPLICABLE;

public class SubmissionMemberConfirmationNotApplicableException extends CustomException {
    public SubmissionMemberConfirmationNotApplicableException() {
        super(SUBMISSION_MEMBER_CONFIRMATION_NOT_APPLICABLE);
    }
}
