package kgu.developers.domain.teamMember.exception;

import static kgu.developers.domain.teamMember.exception.TeamMemberDomainExceptionCode.TEAM_MEMBER_ALREADY_EXISTS;

import kgu.developers.common.exception.CustomException;

public class TeamMemberAlreadyExistsException extends CustomException {
    public TeamMemberAlreadyExistsException() {
        super(TEAM_MEMBER_ALREADY_EXISTS);
    }
}
