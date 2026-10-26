package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachment;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachmentRepository;

public class FakeSectionAnnouncementAttachmentRepository implements SectionAnnouncementAttachmentRepository {

    private final Map<Long, SectionAnnouncementAttachment> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public SectionAnnouncementAttachment save(SectionAnnouncementAttachment attachment) {
        Long id = attachment.getId() != null ? attachment.getId() : sequence.incrementAndGet();

        SectionAnnouncementAttachment saved = SectionAnnouncementAttachment.builder()
            .id(id)
            .announcementId(attachment.getAnnouncementId())
            .fileId(attachment.getFileId())
            .createdAt(attachment.getCreatedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<SectionAnnouncementAttachment> findByIdAndAnnouncementId(Long id, Long announcementId) {
        return Optional.ofNullable(store.get(id))
            .filter(attachment -> attachment.getAnnouncementId().equals(announcementId));
    }

    @Override
    public List<SectionAnnouncementAttachment> findAllByAnnouncementId(Long announcementId) {
        return store.values().stream()
            .filter(attachment -> attachment.getAnnouncementId().equals(announcementId))
            .toList();
    }

    @Override
    public List<SectionAnnouncementAttachment> findAllByAnnouncementIdIn(List<Long> announcementIds) {
        return store.values().stream()
            .filter(attachment -> announcementIds.contains(attachment.getAnnouncementId()))
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
