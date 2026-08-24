package kgu.developers.domain.sectionannouncement.exception;

import static kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementExceptionCode.SECTION_ANNOUNCEMENT_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class SectionAnnouncementNotFoundException extends CustomException {

    public SectionAnnouncementNotFoundException() {
        super(SECTION_ANNOUNCEMENT_NOT_FOUND);
    }
}
