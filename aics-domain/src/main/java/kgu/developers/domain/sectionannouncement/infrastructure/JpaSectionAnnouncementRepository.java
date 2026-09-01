package kgu.developers.domain.sectionannouncement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSectionAnnouncementRepository extends JpaRepository<SectionAnnouncementJpaEntity, Long> {

    List<SectionAnnouncementJpaEntity> findAllBySectionIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(
        Long sectionId, LocalDateTime now
    );

    List<SectionAnnouncementJpaEntity> findAllByPublishedAtLessThanEqualAndNotifiedAtIsNull(LocalDateTime now);
}
