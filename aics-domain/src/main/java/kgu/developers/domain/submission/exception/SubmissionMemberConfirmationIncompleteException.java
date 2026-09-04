package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_MEMBER_CONFIRMATION_INCOMPLETE;

public class SubmissionMemberConfirmationIncompleteException extends CustomException {
    public SubmissionMemberConfirmationIncompleteException() {
        super(SUBMISSION_MEMBER_CONFIRMATION_INCOMPLETE);
    }
}
