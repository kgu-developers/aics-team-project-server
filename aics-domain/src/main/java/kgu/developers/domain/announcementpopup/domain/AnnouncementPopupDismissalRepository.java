package kgu.developers.domain.announcementpopup.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementPopupDismissalRepository {
    List<AnnouncementPopupDismissal> findAllByUserId(String userId);

    AnnouncementPopupDismissal save(AnnouncementPopupDismissal dismissal, LocalDateTime now);
}
