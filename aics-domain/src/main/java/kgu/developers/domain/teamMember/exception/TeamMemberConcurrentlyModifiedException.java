package kgu.developers.domain.teamMember.exception;

import static kgu.developers.domain.teamMember.exception.TeamMemberDomainExceptionCode.TEAM_MEMBER_CONCURRENTLY_MODIFIED;

import kgu.developers.common.exception.CustomException;

public class TeamMemberConcurrentlyModifiedException extends CustomException {
    public TeamMemberConcurrentlyModifiedException() {
        super(TEAM_MEMBER_CONCURRENTLY_MODIFIED);
    }
}
