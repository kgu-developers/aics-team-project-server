package kgu.developers.domain.announcementpopup.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAnnouncementPopupRepository extends JpaRepository<AnnouncementPopupJpaEntity, Long> {

    boolean existsByAnnouncementId(Long announcementId);

    List<AnnouncementPopupJpaEntity> findAllBySectionIdOrderByIdDesc(Long sectionId);
}
