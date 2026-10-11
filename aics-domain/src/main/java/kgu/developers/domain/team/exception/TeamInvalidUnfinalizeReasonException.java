package kgu.developers.domain.team.exception;

import static kgu.developers.domain.team.exception.TeamDomainExceptionCode.TEAM_INVALID_UNFINALIZE_REASON;

import kgu.developers.common.exception.CustomException;

public class TeamInvalidUnfinalizeReasonException extends CustomException {
    public TeamInvalidUnfinalizeReasonException() {
        super(TEAM_INVALID_UNFINALIZE_REASON);
    }
}
