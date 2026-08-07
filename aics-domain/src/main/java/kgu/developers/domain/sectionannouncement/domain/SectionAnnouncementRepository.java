package kgu.developers.domain.sectionannouncement.domain;

import java.util.List;
import java.util.Optional;

public interface SectionAnnouncementRepository {

    SectionAnnouncement save(SectionAnnouncement sectionAnnouncement);

    Optional<SectionAnnouncement> findById(Long id);

    List<SectionAnnouncement> findAllBySectionId(Long sectionId);

    void deleteById(Long id);
}
