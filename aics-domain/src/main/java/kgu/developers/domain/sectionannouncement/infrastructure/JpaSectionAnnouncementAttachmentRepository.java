package kgu.developers.domain.sectionannouncement.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSectionAnnouncementAttachmentRepository
    extends JpaRepository<SectionAnnouncementAttachmentJpaEntity, Long> {

    Optional<SectionAnnouncementAttachmentJpaEntity> findByIdAndAnnouncementId(Long id, Long announcementId);

    List<SectionAnnouncementAttachmentJpaEntity> findAllByAnnouncementId(Long announcementId);

    List<SectionAnnouncementAttachmentJpaEntity> findAllByAnnouncementIdIn(List<Long> announcementIds);
}
