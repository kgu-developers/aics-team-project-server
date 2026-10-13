package kgu.developers.domain.team.exception;

import static kgu.developers.domain.team.exception.TeamDomainExceptionCode.TEAM_UNFINALIZE_COMPLETED_FINAL_REPORT;

import kgu.developers.common.exception.CustomException;

public class TeamUnfinalizeCompletedFinalReportException extends CustomException {
    public TeamUnfinalizeCompletedFinalReportException() {
        super(TEAM_UNFINALIZE_COMPLETED_FINAL_REPORT);
    }
}
