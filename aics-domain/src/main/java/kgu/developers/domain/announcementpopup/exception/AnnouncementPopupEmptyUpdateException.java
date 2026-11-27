package kgu.developers.domain.announcementpopup.exception;

import static kgu.developers.domain.announcementpopup.exception.AnnouncementPopupExceptionCode.ANNOUNCEMENT_POPUP_EMPTY_UPDATE;

import kgu.developers.common.exception.CustomException;

public class AnnouncementPopupEmptyUpdateException extends CustomException {

    public AnnouncementPopupEmptyUpdateException() {
        super(ANNOUNCEMENT_POPUP_EMPTY_UPDATE);
    }
}
