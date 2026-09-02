package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_SECTION_ACCESS_FORBIDDEN;

import kgu.developers.common.exception.CustomException;

public class MilestoneSectionAccessDeniedException extends CustomException {
    public MilestoneSectionAccessDeniedException() {
        super(MILESTONE_SECTION_ACCESS_FORBIDDEN);
    }
}
