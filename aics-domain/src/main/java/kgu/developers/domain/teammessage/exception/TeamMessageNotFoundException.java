package kgu.developers.domain.teammessage.exception;

import static kgu.developers.domain.teammessage.exception.TeamMessageExceptionCode.TEAM_MESSAGE_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class TeamMessageNotFoundException extends CustomException {

    public TeamMessageNotFoundException() {
        super(TEAM_MESSAGE_NOT_FOUND);
    }
}
