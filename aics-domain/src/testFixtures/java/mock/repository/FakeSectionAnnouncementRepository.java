package mock.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementRepository;

public class FakeSectionAnnouncementRepository implements SectionAnnouncementRepository {

    private final Map<Long, SectionAnnouncement> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public SectionAnnouncement save(SectionAnnouncement sectionAnnouncement) {
        Long id = sectionAnnouncement.getId() != null ? sectionAnnouncement.getId() : sequence.incrementAndGet();

        SectionAnnouncement saved = SectionAnnouncement.builder()
            .id(id)
            .sectionId(sectionAnnouncement.getSectionId())
            .title(sectionAnnouncement.getTitle())
            .content(sectionAnnouncement.getContent())
            .publishedAt(sectionAnnouncement.getPublishedAt())
            .version(sectionAnnouncement.getVersion())
            .createdAt(sectionAnnouncement.getCreatedAt() != null ? sectionAnnouncement.getCreatedAt() : LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<SectionAnnouncement> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<SectionAnnouncement> findAllPublishedBySectionId(Long sectionId, LocalDateTime now) {
        return store.values().stream()
            .filter(announcement -> announcement.getSectionId().equals(sectionId))
            .filter(announcement -> !announcement.getPublishedAt().isAfter(now))
            .sorted(Comparator.comparing(SectionAnnouncement::getPublishedAt).reversed())
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }
}
