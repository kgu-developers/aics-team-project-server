package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class MidReportVersionConflictException extends CustomException {
    public MidReportVersionConflictException() {
        super(MidReportExceptionCode.VERSION_CONFLICT);
    }

    public MidReportVersionConflictException(Throwable cause) {
        super(MidReportExceptionCode.VERSION_CONFLICT, cause);
    }
}
