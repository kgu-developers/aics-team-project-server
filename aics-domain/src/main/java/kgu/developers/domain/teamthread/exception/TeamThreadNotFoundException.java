package kgu.developers.domain.teamthread.exception;

import static kgu.developers.domain.teamthread.exception.TeamThreadExceptionCode.TEAM_THREAD_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class TeamThreadNotFoundException extends CustomException {

    public TeamThreadNotFoundException() {
        super(TEAM_THREAD_NOT_FOUND);
    }
}
