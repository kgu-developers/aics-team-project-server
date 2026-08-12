package kgu.developers.domain.section.exception;

import static kgu.developers.domain.section.exception.SectionDomainExceptionCode.SECTION_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class SectionNotFoundException extends CustomException {
    public SectionNotFoundException() {
        super(SECTION_NOT_FOUND);
    }
}
