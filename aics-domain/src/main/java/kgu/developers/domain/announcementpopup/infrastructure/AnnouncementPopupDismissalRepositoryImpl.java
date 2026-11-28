package kgu.developers.domain.announcementpopup.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissal;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnnouncementPopupDismissalRepositoryImpl implements AnnouncementPopupDismissalRepository {
    private final JpaAnnouncementPopupDismissalRepository repository;

    @Override
    public List<AnnouncementPopupDismissal> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId).stream().map(AnnouncementPopupDismissalJpaEntity::toDomain).toList();
    }

    @Override
    public AnnouncementPopupDismissal save(AnnouncementPopupDismissal dismissal, LocalDateTime now) {
        return repository.upsert(dismissal.userId(), dismissal.popupId(), dismissal.contentVersion(),
            dismissal.hiddenUntil(), now).toDomain();
    }
}
