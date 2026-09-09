package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class InvalidMidReportFieldsException extends CustomException {
    public InvalidMidReportFieldsException() {
        super(MidReportExceptionCode.INVALID_MID_REPORT_FIELDS);
    }
}
