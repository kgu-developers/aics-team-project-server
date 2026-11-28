package kgu.developers.domain.announcementpopup.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.domain.StudentAnnouncementPopupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudentAnnouncementPopupRepositoryImpl implements StudentAnnouncementPopupRepository {
    private final JpaStudentAnnouncementPopupRepository repository;

    @Override
    public List<AnnouncementPopup> findAvailable(String userId, Long popupId, LocalDateTime now) {
        return repository.findAvailable(userId, popupId, now).stream()
            .map(AnnouncementPopupJpaEntity::toDomain).toList();
    }
}
