package kgu.developers.domain.teamMember.exception;

import static kgu.developers.domain.teamMember.exception.TeamMemberDomainExceptionCode.TEAM_MEMBER_SECTION_MISMATCH;

import kgu.developers.common.exception.CustomException;

public class TeamMemberSectionMismatchException extends CustomException {
    public TeamMemberSectionMismatchException() {
        super(TEAM_MEMBER_SECTION_MISMATCH);
    }
}
