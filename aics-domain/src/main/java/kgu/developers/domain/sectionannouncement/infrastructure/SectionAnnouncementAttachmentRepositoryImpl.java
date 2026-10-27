package kgu.developers.domain.sectionannouncement.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachment;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SectionAnnouncementAttachmentRepositoryImpl implements SectionAnnouncementAttachmentRepository {

    private final JpaSectionAnnouncementAttachmentRepository jpaSectionAnnouncementAttachmentRepository;

    @Override
    public SectionAnnouncementAttachment save(SectionAnnouncementAttachment attachment) {
        return jpaSectionAnnouncementAttachmentRepository.save(SectionAnnouncementAttachmentJpaEntity.toEntity(attachment))
            .toDomain();
    }

    @Override
    public Optional<SectionAnnouncementAttachment> findByIdAndAnnouncementId(Long id, Long announcementId) {
        return jpaSectionAnnouncementAttachmentRepository.findByIdAndAnnouncementId(id, announcementId)
            .map(SectionAnnouncementAttachmentJpaEntity::toDomain);
    }

    @Override
    public List<SectionAnnouncementAttachment> findAllByAnnouncementId(Long announcementId) {
        return jpaSectionAnnouncementAttachmentRepository.findAllByAnnouncementId(announcementId).stream()
            .map(SectionAnnouncementAttachmentJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<SectionAnnouncementAttachment> findAllByAnnouncementIdIn(List<Long> announcementIds) {
        return jpaSectionAnnouncementAttachmentRepository.findAllByAnnouncementIdIn(announcementIds).stream()
            .map(SectionAnnouncementAttachmentJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaSectionAnnouncementAttachmentRepository.deleteById(id);
    }
}
