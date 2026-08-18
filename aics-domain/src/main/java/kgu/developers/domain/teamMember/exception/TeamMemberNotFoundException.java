package kgu.developers.domain.teamMember.exception;

import static kgu.developers.domain.teamMember.exception.TeamMemberDomainExceptionCode.TEAM_MEMBER_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class TeamMemberNotFoundException extends CustomException {
    public TeamMemberNotFoundException() {
        super(TEAM_MEMBER_NOT_FOUND);
    }
}
