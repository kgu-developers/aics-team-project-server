package kgu.developers.domain.sectionannouncement.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementRepository;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementConcurrentlyModifiedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SectionAnnouncementRepositoryImpl implements SectionAnnouncementRepository {

    private final JpaSectionAnnouncementRepository jpaSectionAnnouncementRepository;

    @Override
    public SectionAnnouncement save(SectionAnnouncement sectionAnnouncement) {
        try {
            return jpaSectionAnnouncementRepository.saveAndFlush(SectionAnnouncementJpaEntity.toEntity(sectionAnnouncement)).toDomain();
        } catch (OptimisticLockingFailureException e) {
            throw new SectionAnnouncementConcurrentlyModifiedException();
        }
    }

    @Override
    public Optional<SectionAnnouncement> findById(Long id) {
        return jpaSectionAnnouncementRepository.findById(id).map(SectionAnnouncementJpaEntity::toDomain);
    }

    @Override
    public List<SectionAnnouncement> findAllPublishedBySectionId(Long sectionId, LocalDateTime now) {
        return jpaSectionAnnouncementRepository
            .findAllBySectionIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(sectionId, now).stream()
            .map(SectionAnnouncementJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<SectionAnnouncement> findAllToNotify(LocalDateTime now) {
        return jpaSectionAnnouncementRepository.findAllByPublishedAtLessThanEqualAndNotifiedAtIsNull(now).stream()
            .map(SectionAnnouncementJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaSectionAnnouncementRepository.deleteById(id);
    }
}
