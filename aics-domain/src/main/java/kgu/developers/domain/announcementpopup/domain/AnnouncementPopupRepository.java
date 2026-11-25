package kgu.developers.domain.announcementpopup.domain;

import java.util.List;
import java.util.Optional;

public interface AnnouncementPopupRepository {

    AnnouncementPopup save(AnnouncementPopup announcementPopup);

    Optional<AnnouncementPopup> findById(Long id);

    boolean existsByAnnouncementId(Long announcementId);

    /** 분반의 팝업을 최신순으로 반환한다. 꺼진 팝업도 포함한다(교수 관리 화면용). */
    List<AnnouncementPopup> findAllBySectionId(Long sectionId);
}
