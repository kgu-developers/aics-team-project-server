package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class MidReportBlockNotFoundException extends CustomException {
    public MidReportBlockNotFoundException() {
        super(MidReportExceptionCode.MID_REPORT_BLOCK_NOT_FOUND);
    }
}
