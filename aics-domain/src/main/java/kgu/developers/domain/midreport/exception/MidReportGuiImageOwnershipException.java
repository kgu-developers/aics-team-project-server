package kgu.developers.domain.midreport.exception;

import static kgu.developers.domain.midreport.exception.MidReportExceptionCode.MID_REPORT_GUI_IMAGE_NOT_OWNED;

import kgu.developers.common.exception.CustomException;

public class MidReportGuiImageOwnershipException extends CustomException {
    public MidReportGuiImageOwnershipException() {
        super(MID_REPORT_GUI_IMAGE_NOT_OWNED);
    }
}
