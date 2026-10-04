package kgu.developers.domain.milestone.exception;

import kgu.developers.common.exception.CustomException;

public class MilestoneEvaluationWindowConflictException extends CustomException {
    public MilestoneEvaluationWindowConflictException() {
        super(MilestoneDomainExceptionCode.MILESTONE_EVALUATION_WINDOW_CONFLICT);
    }
}
