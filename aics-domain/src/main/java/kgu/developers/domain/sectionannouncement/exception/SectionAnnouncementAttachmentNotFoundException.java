package kgu.developers.domain.sectionannouncement.exception;

import static kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementExceptionCode.SECTION_ANNOUNCEMENT_ATTACHMENT_NOT_FOUND;

import kgu.developers.common.exception.CustomException;

public class SectionAnnouncementAttachmentNotFoundException extends CustomException {

    public SectionAnnouncementAttachmentNotFoundException() {
        super(SECTION_ANNOUNCEMENT_ATTACHMENT_NOT_FOUND);
    }
}
