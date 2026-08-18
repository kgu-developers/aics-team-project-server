package kgu.developers.domain.team.exception;

import static kgu.developers.domain.team.exception.TeamDomainExceptionCode.TEAM_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class TeamNotFoundException extends CustomException {
    public TeamNotFoundException() {
        super(TEAM_NOT_FOUND);
    }
}
