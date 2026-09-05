package kgu.developers.domain.project.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROJECT_APPROVAL_REQUIRED;

public class ProjectApprovalRequiredException extends CustomException {
    public ProjectApprovalRequiredException() {
        super(PROJECT_APPROVAL_REQUIRED);
    }
}
