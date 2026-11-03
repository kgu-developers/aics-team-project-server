package kgu.developers.domain.teammessage.exception;

import static kgu.developers.domain.teammessage.exception.TeamMessageExceptionCode.TEAM_MESSAGE_NOT_GROUPED;

import kgu.developers.common.exception.CustomException;

public class TeamMessageNotGroupedException extends CustomException {

    public TeamMessageNotGroupedException() {
        super(TEAM_MESSAGE_NOT_GROUPED);
    }
}
