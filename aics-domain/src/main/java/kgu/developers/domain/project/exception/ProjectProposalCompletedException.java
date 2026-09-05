package kgu.developers.domain.project.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROJECT_PROPOSAL_COMPLETED;

public class ProjectProposalCompletedException extends CustomException {
    public ProjectProposalCompletedException() {
        super(PROJECT_PROPOSAL_COMPLETED);
    }
}
