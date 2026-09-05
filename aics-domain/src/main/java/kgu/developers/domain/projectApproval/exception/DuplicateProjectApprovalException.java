package kgu.developers.domain.projectApproval.exception;

import static kgu.developers.domain.projectApproval.exception.ProjectApprovalDomainExceptionCode.DUPLICATE_PROJECT_APPROVAL;

import kgu.developers.common.exception.CustomException;

public class DuplicateProjectApprovalException extends CustomException {
    public DuplicateProjectApprovalException() {
        super(DUPLICATE_PROJECT_APPROVAL);
    }

    public DuplicateProjectApprovalException(Throwable cause) {
        super(DUPLICATE_PROJECT_APPROVAL, cause);
    }
}
