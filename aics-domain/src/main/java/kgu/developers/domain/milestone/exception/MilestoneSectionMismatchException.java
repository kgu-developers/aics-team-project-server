package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_SECTION_FORBIDDEN;

import kgu.developers.common.exception.CustomException;
import lombok.Getter;

@Getter
public class MilestoneSectionMismatchException extends CustomException {
    private final Long milestoneId;
    private final Long sectionId;

    public MilestoneSectionMismatchException(Long milestoneId, Long sectionId) {
        super(MILESTONE_SECTION_FORBIDDEN);
        this.milestoneId = milestoneId;
        this.sectionId = sectionId;
    }
}
