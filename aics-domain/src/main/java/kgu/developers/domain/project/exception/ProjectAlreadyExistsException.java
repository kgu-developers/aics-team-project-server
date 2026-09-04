package kgu.developers.domain.project.exception;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROJECT_ALREADY_EXISTS;

import kgu.developers.common.exception.CustomException;

public class ProjectAlreadyExistsException extends CustomException {
    public ProjectAlreadyExistsException() {
        super(PROJECT_ALREADY_EXISTS);
    }
}
