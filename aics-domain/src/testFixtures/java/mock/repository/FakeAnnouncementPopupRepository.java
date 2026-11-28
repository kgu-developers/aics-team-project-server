package mock.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupRepository;

public class FakeAnnouncementPopupRepository implements AnnouncementPopupRepository {

    private final Map<Long, AnnouncementPopup> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public AnnouncementPopup save(AnnouncementPopup announcementPopup) {
        Long id = announcementPopup.getId() != null ? announcementPopup.getId() : sequence.incrementAndGet();

        AnnouncementPopup saved = AnnouncementPopup.builder()
            .id(id)
            .announcementId(announcementPopup.getAnnouncementId())
            .sectionId(announcementPopup.getSectionId())
            .title(announcementPopup.getTitle())
            .content(announcementPopup.getContent())
            .imageFileId(announcementPopup.getImageFileId())
            .enabled(announcementPopup.isEnabled())
            .version(announcementPopup.getVersion())
            .contentVersion(announcementPopup.getContentVersion())
            .createdAt(announcementPopup.getCreatedAt() != null ? announcementPopup.getCreatedAt() : LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<AnnouncementPopup> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(this::copyOf);
    }

    @Override
    public boolean existsByAnnouncementId(Long announcementId) {
        return store.values().stream().anyMatch(popup -> popup.getAnnouncementId().equals(announcementId));
    }

    @Override
    public List<AnnouncementPopup> findAllBySectionId(Long sectionId) {
        return store.values().stream()
            .filter(popup -> popup.getSectionId().equals(sectionId))
            .sorted(Comparator.comparing(AnnouncementPopup::getId).reversed())
            .map(this::copyOf)
            .toList();
    }

    // 저장소 밖에서 도메인 객체를 바꿔도 저장된 값이 함께 바뀌지 않도록, 실제 JPA처럼 매번 새 객체를 돌려준다.
    // (이게 없으면 save 없이 수정한 값이 조회에 비치는 버그를 테스트가 놓친다.)
    private AnnouncementPopup copyOf(AnnouncementPopup popup) {
        return AnnouncementPopup.builder()
            .id(popup.getId())
            .announcementId(popup.getAnnouncementId())
            .sectionId(popup.getSectionId())
            .title(popup.getTitle())
            .content(popup.getContent())
            .imageFileId(popup.getImageFileId())
            .enabled(popup.isEnabled())
            .version(popup.getVersion())
            .contentVersion(popup.getContentVersion())
            .createdAt(popup.getCreatedAt())
            .updatedAt(popup.getUpdatedAt())
            .build();
    }
}
