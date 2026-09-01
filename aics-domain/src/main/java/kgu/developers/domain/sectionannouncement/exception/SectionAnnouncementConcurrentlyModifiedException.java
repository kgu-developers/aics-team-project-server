package kgu.developers.domain.sectionannouncement.exception;

import static kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementExceptionCode.SECTION_ANNOUNCEMENT_CONCURRENTLY_MODIFIED;

import kgu.developers.common.exception.CustomException;

public class SectionAnnouncementConcurrentlyModifiedException extends CustomException {

    public SectionAnnouncementConcurrentlyModifiedException() {
        super(SECTION_ANNOUNCEMENT_CONCURRENTLY_MODIFIED);
    }
}
