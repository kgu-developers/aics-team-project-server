package kgu.developers.domain.sectionannouncement.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SectionAnnouncementRepository {

    SectionAnnouncement save(SectionAnnouncement sectionAnnouncement);

    Optional<SectionAnnouncement> findById(Long id);

    /** publishedAt이 now 이전(예약 게시 시각이 지난 것)인 것만, 최신순으로 반환한다. */
    List<SectionAnnouncement> findAllPublishedBySectionId(Long sectionId, LocalDateTime now);

    void deleteById(Long id);
}
