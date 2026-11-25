package kgu.developers.domain.announcementpopup.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupRepository;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupAlreadyExistsException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupConcurrentlyModifiedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnnouncementPopupRepositoryImpl implements AnnouncementPopupRepository {

    private final JpaAnnouncementPopupRepository jpaAnnouncementPopupRepository;

    @Override
    public AnnouncementPopup save(AnnouncementPopup announcementPopup) {
        try {
            return jpaAnnouncementPopupRepository
                .saveAndFlush(AnnouncementPopupJpaEntity.toEntity(announcementPopup))
                .toDomain();
        } catch (OptimisticLockingFailureException e) {
            throw new AnnouncementPopupConcurrentlyModifiedException();
        } catch (DataIntegrityViolationException e) {
            // NOT NULL 칼럼은 서비스에서 먼저 검증하므로, 남는 무결성 위반은 공지당 팝업 1개 유일 제약
            // (uk_announcement_popup_announcement)을 동시 요청이 먼저 채운 경우다.
            throw new AnnouncementPopupAlreadyExistsException();
        }
    }

    @Override
    public Optional<AnnouncementPopup> findById(Long id) {
        return jpaAnnouncementPopupRepository.findById(id).map(AnnouncementPopupJpaEntity::toDomain);
    }

    @Override
    public boolean existsByAnnouncementId(Long announcementId) {
        return jpaAnnouncementPopupRepository.existsByAnnouncementId(announcementId);
    }

    @Override
    public List<AnnouncementPopup> findAllBySectionId(Long sectionId) {
        return jpaAnnouncementPopupRepository.findAllBySectionIdOrderByIdDesc(sectionId).stream()
            .map(AnnouncementPopupJpaEntity::toDomain)
            .toList();
    }
}
