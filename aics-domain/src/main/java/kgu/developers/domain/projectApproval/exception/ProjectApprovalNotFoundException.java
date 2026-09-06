package kgu.developers.domain.projectApproval.exception;

import static kgu.developers.domain.projectApproval.exception.ProjectApprovalDomainExceptionCode.PROJECT_APPROVAL_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class ProjectApprovalNotFoundException extends CustomException {
    public ProjectApprovalNotFoundException() {
        super(PROJECT_APPROVAL_NOT_FOUND);
    }
}
