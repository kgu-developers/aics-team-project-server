package kgu.developers.domain.teamMember.exception;

import static kgu.developers.domain.teamMember.exception.TeamMemberDomainExceptionCode.LEADER_ALREADY_EXISTS;

import kgu.developers.common.exception.CustomException;

public class LeaderAlreadyExistsException extends CustomException {
    public LeaderAlreadyExistsException() {
        super(LEADER_ALREADY_EXISTS);
    }
}
