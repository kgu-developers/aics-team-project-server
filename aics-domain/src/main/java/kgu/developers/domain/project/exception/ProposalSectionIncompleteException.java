package kgu.developers.domain.project.exception;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROPOSAL_SECTION_INCOMPLETE;

import kgu.developers.common.exception.CustomException;

public class ProposalSectionIncompleteException extends CustomException {
    public ProposalSectionIncompleteException() {
        super(PROPOSAL_SECTION_INCOMPLETE);
    }
}
