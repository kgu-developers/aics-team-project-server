package kgu.developers.domain.team.exception;

import static kgu.developers.domain.team.exception.TeamDomainExceptionCode.TEAM_ALREADY_CONFIRMED;

import kgu.developers.common.exception.CustomException;

public class TeamAlreadyConfirmedException extends CustomException {
    public TeamAlreadyConfirmedException() {
        super(TEAM_ALREADY_CONFIRMED);
    }
}
