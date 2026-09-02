package kgu.developers.domain.team.exception;

import static kgu.developers.domain.team.exception.TeamDomainExceptionCode.DUPLICATE_TEAM_NAME;

import kgu.developers.common.exception.CustomException;

public class DuplicateTeamNameException extends CustomException {
    public DuplicateTeamNameException() {
        super(DUPLICATE_TEAM_NAME);
    }
}
