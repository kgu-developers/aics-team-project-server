package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class MidReportNotFoundException extends CustomException {
    public MidReportNotFoundException() {
        super(MidReportExceptionCode.MID_REPORT_NOT_FOUND);
    }
}
