package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class MidReportRevisionNotCompletableException extends CustomException {
    public MidReportRevisionNotCompletableException() {
        super(MidReportExceptionCode.MID_REPORT_REVISION_NOT_COMPLETABLE);
    }
}
