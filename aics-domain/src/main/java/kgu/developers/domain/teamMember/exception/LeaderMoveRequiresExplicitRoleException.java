package kgu.developers.domain.teamMember.exception;

import static kgu.developers.domain.teamMember.exception.TeamMemberDomainExceptionCode.LEADER_MOVE_REQUIRES_EXPLICIT_ROLE;

import kgu.developers.common.exception.CustomException;

public class LeaderMoveRequiresExplicitRoleException extends CustomException {
    public LeaderMoveRequiresExplicitRoleException() {
        super(LEADER_MOVE_REQUIRES_EXPLICIT_ROLE);
    }
}
