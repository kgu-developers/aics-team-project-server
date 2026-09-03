package kgu.developers.domain.section.exception;

import static kgu.developers.domain.section.exception.SectionDomainExceptionCode.PROFESSOR_ROLE_REQUIRED;

import kgu.developers.common.exception.CustomException;

public class ProfessorRoleRequiredException extends CustomException {
    public ProfessorRoleRequiredException() {
        super(PROFESSOR_ROLE_REQUIRED);
    }
}
