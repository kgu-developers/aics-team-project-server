package kgu.developers.domain.milestone.exception;

import static kgu.developers.domain.milestone.exception.MilestoneDomainExceptionCode.MILESTONE_NOT_FOUND;

import kgu.developers.common.exception.CustomException;
import lombok.Getter;

@Getter
public class MilestoneNotFoundException extends CustomException {
    private final Long milestoneId;

    public MilestoneNotFoundException(Long milestoneId) {
        super(MILESTONE_NOT_FOUND);
        this.milestoneId = milestoneId;
    }
}
