package kgu.developers.domain.announcementpopup.exception;

import static kgu.developers.domain.announcementpopup.exception.AnnouncementPopupExceptionCode.ANNOUNCEMENT_POPUP_IMAGE_CONFLICT;

import kgu.developers.common.exception.CustomException;

public class AnnouncementPopupImageConflictException extends CustomException {

    public AnnouncementPopupImageConflictException() {
        super(ANNOUNCEMENT_POPUP_IMAGE_CONFLICT);
    }
}
