package kgu.developers.domain.sectionannouncement.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSectionAnnouncementRepository extends JpaRepository<SectionAnnouncementJpaEntity, Long> {

    List<SectionAnnouncementJpaEntity> findAllBySectionId(Long sectionId);
}
