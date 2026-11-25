package kgu.developers.domain.announcementpopup.exception;

import static kgu.developers.domain.announcementpopup.exception.AnnouncementPopupExceptionCode.ANNOUNCEMENT_POPUP_TITLE_TOO_LONG;

import kgu.developers.common.exception.CustomException;

public class AnnouncementPopupTitleTooLongException extends CustomException {

    public AnnouncementPopupTitleTooLongException() {
        super(ANNOUNCEMENT_POPUP_TITLE_TOO_LONG);
    }
}
