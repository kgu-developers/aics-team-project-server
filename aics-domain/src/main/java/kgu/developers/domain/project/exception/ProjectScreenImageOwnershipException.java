package kgu.developers.domain.project.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROJECT_SCREEN_IMAGE_OWNERSHIP_INVALID;

public class ProjectScreenImageOwnershipException extends CustomException {
    public ProjectScreenImageOwnershipException() {
        super(PROJECT_SCREEN_IMAGE_OWNERSHIP_INVALID);
    }
}
