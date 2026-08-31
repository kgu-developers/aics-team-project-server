package kgu.developers.domain.sectionannouncement.exception;

import static kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementExceptionCode.SECTION_ANNOUNCEMENT_INVALID_CONTENT;

import kgu.developers.common.exception.CustomException;

public class SectionAnnouncementInvalidContentException extends CustomException {

    public SectionAnnouncementInvalidContentException() {
        super(SECTION_ANNOUNCEMENT_INVALID_CONTENT);
    }
}
