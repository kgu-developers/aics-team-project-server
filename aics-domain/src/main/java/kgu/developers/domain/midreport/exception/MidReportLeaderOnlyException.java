package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class MidReportLeaderOnlyException extends CustomException {
    public MidReportLeaderOnlyException() {
        super(MidReportExceptionCode.MID_REPORT_LEADER_ONLY);
    }
}
