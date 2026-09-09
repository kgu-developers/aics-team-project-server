package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class MidReportSubmittedException extends CustomException {
    public MidReportSubmittedException() {
        super(MidReportExceptionCode.MID_REPORT_SUBMITTED);
    }
}
