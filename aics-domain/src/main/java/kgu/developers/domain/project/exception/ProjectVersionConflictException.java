package kgu.developers.domain.project.exception;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROJECT_VERSION_CONFLICT;

import kgu.developers.common.exception.CustomException;

public class ProjectVersionConflictException extends CustomException {
    public ProjectVersionConflictException() {
        super(PROJECT_VERSION_CONFLICT);
    }
}
