package kgu.developers.domain.announcementpopup.exception;

import static kgu.developers.domain.announcementpopup.exception.AnnouncementPopupExceptionCode.ANNOUNCEMENT_POPUP_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class AnnouncementPopupNotFoundException extends CustomException {

    public AnnouncementPopupNotFoundException() {
        super(ANNOUNCEMENT_POPUP_NOT_FOUND);
    }
}
