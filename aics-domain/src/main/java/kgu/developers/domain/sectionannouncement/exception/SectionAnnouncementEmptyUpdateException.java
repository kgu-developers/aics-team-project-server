package kgu.developers.domain.sectionannouncement.exception;

import static kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementExceptionCode.SECTION_ANNOUNCEMENT_EMPTY_UPDATE;

import kgu.developers.common.exception.CustomException;

public class SectionAnnouncementEmptyUpdateException extends CustomException {

    public SectionAnnouncementEmptyUpdateException() {
        super(SECTION_ANNOUNCEMENT_EMPTY_UPDATE);
    }
}
