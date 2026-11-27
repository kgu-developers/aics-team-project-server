package kgu.developers.domain.announcementpopup.exception;

import static kgu.developers.domain.announcementpopup.exception.AnnouncementPopupExceptionCode.ANNOUNCEMENT_POPUP_ALREADY_EXISTS;

import kgu.developers.common.exception.CustomException;

public class AnnouncementPopupAlreadyExistsException extends CustomException {

    public AnnouncementPopupAlreadyExistsException() {
        super(ANNOUNCEMENT_POPUP_ALREADY_EXISTS);
    }
}
