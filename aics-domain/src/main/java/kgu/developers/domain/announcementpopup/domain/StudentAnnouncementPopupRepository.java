package kgu.developers.domain.announcementpopup.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface StudentAnnouncementPopupRepository {
    // popupId가 null이면 전체, 지정하면 해당 팝업만. 숨김 적용 전의 접근 가능한 팝업이다.
    List<AnnouncementPopup> findAvailable(String userId, Long popupId, LocalDateTime now);
}
