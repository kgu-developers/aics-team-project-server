package kgu.developers.domain.submission.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_LEADER_ONLY;

public class SubmissionLeaderOnlyException extends CustomException {
    public SubmissionLeaderOnlyException() {
        super(SUBMISSION_LEADER_ONLY);
    }
}
