package kgu.developers.domain.project.exception;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROPOSAL_SECTION_ASSIGNEE_NOT_MEMBER;

import kgu.developers.common.exception.CustomException;

public class ProposalSectionAssigneeNotMemberException extends CustomException {
    public ProposalSectionAssigneeNotMemberException() {
        super(PROPOSAL_SECTION_ASSIGNEE_NOT_MEMBER);
    }
}
