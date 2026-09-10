package kgu.developers.domain.project.exception;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROJECT_DELETED_CONCURRENTLY;

import kgu.developers.common.exception.CustomException;

public class ProjectDeletedConcurrentlyException extends CustomException {
    public ProjectDeletedConcurrentlyException() {
        super(PROJECT_DELETED_CONCURRENTLY);
    }
}
