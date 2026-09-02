package kgu.developers.domain.team.exception;

import static kgu.developers.domain.team.exception.TeamDomainExceptionCode.TEAM_CONCURRENTLY_MODIFIED;

import kgu.developers.common.exception.CustomException;

public class TeamConcurrentlyModifiedException extends CustomException {
    public TeamConcurrentlyModifiedException() {
        super(TEAM_CONCURRENTLY_MODIFIED);
    }
}
