package kgu.developers.domain.sectionannouncement.domain;

import java.util.List;
import java.util.Optional;

public interface SectionAnnouncementAttachmentRepository {

    SectionAnnouncementAttachment save(SectionAnnouncementAttachment attachment);

    Optional<SectionAnnouncementAttachment> findByIdAndAnnouncementId(Long id, Long announcementId);

    List<SectionAnnouncementAttachment> findAllByAnnouncementId(Long announcementId);

    List<SectionAnnouncementAttachment> findAllByAnnouncementIdIn(List<Long> announcementIds);

    void deleteById(Long id);
}
