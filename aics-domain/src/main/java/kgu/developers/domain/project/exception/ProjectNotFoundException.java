package kgu.developers.domain.project.exception;

import static kgu.developers.domain.project.exception.ProjectDomainExceptionCode.PROJECT_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class ProjectNotFoundException extends CustomException {
    public ProjectNotFoundException() {
        super(PROJECT_NOT_FOUND);
    }
}