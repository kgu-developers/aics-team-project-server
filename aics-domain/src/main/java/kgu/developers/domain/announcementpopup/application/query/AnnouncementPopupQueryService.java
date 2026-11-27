package kgu.developers.domain.announcementpopup.application.query;

import java.util.List;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupRepository;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnnouncementPopupQueryService {

    private final AnnouncementPopupRepository announcementPopupRepository;

    public AnnouncementPopup getPopup(Long id) {
        return announcementPopupRepository.findById(id)
            .orElseThrow(AnnouncementPopupNotFoundException::new);
    }

    /** 꺼진 팝업을 포함한 분반의 전체 팝업(최신순). 교수 관리 화면용이다. */
    public List<AnnouncementPopup> getPopups(Long sectionId) {
        return announcementPopupRepository.findAllBySectionId(sectionId);
    }
}
