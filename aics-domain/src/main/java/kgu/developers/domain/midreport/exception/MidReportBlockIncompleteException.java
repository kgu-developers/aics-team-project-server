package kgu.developers.domain.midreport.exception;

import kgu.developers.common.exception.CustomException;

public class MidReportBlockIncompleteException extends CustomException {
    public MidReportBlockIncompleteException() {
        super(MidReportExceptionCode.BLOCK_INCOMPLETE);
    }
}
