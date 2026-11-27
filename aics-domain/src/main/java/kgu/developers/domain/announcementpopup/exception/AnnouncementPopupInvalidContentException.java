package kgu.developers.domain.announcementpopup.exception;

import static kgu.developers.domain.announcementpopup.exception.AnnouncementPopupExceptionCode.ANNOUNCEMENT_POPUP_INVALID_CONTENT;

import kgu.developers.common.exception.CustomException;

public class AnnouncementPopupInvalidContentException extends CustomException {

    public AnnouncementPopupInvalidContentException() {
        super(ANNOUNCEMENT_POPUP_INVALID_CONTENT);
    }
}
