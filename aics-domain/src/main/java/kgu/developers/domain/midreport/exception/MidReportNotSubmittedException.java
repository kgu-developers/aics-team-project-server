package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

import static kgu.developers.domain.midreport.exception.MidReportExceptionCode.MID_REPORT_NOT_SUBMITTED;

public class MidReportNotSubmittedException extends CustomException {
    public MidReportNotSubmittedException() {
        super(MID_REPORT_NOT_SUBMITTED);
    }
}
