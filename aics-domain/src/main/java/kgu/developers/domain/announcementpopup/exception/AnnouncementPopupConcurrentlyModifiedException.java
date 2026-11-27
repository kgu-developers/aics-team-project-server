package kgu.developers.domain.announcementpopup.exception;

import static kgu.developers.domain.announcementpopup.exception.AnnouncementPopupExceptionCode.ANNOUNCEMENT_POPUP_CONCURRENTLY_MODIFIED;

import kgu.developers.common.exception.CustomException;

public class AnnouncementPopupConcurrentlyModifiedException extends CustomException {

    public AnnouncementPopupConcurrentlyModifiedException() {
        super(ANNOUNCEMENT_POPUP_CONCURRENTLY_MODIFIED);
    }
}
