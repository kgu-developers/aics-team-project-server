package kgu.developers.domain.submission.exception;

import static kgu.developers.domain.submission.exception.SubmissionExceptionCode.SUBMISSION_TEAM_NOT_CONFIRMED;

import kgu.developers.common.exception.CustomException;

public class SubmissionTeamNotConfirmedException extends CustomException {
    public SubmissionTeamNotConfirmedException() {
        super(SUBMISSION_TEAM_NOT_CONFIRMED);
    }
}
