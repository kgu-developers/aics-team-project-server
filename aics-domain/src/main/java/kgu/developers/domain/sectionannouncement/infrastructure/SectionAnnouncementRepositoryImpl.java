package kgu.developers.domain.sectionannouncement.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SectionAnnouncementRepositoryImpl implements SectionAnnouncementRepository {

    private final JpaSectionAnnouncementRepository jpaSectionAnnouncementRepository;

    @Override
    public SectionAnnouncement save(SectionAnnouncement sectionAnnouncement) {
        return jpaSectionAnnouncementRepository.save(SectionAnnouncementJpaEntity.toEntity(sectionAnnouncement)).toDomain();
    }

    @Override
    public Optional<SectionAnnouncement> findById(Long id) {
        return jpaSectionAnnouncementRepository.findById(id).map(SectionAnnouncementJpaEntity::toDomain);
    }

    @Override
    public List<SectionAnnouncement> findAllBySectionId(Long sectionId) {
        return jpaSectionAnnouncementRepository.findAllBySectionId(sectionId).stream()
            .map(SectionAnnouncementJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaSectionAnnouncementRepository.deleteById(id);
    }
}
