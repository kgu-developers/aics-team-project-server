package kgu.developers.domain.sectionannouncement.application.query;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementRepository;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SectionAnnouncementQueryService {

    private final SectionAnnouncementRepository sectionAnnouncementRepository;

    public SectionAnnouncement getAnnouncement(Long id) {
        return sectionAnnouncementRepository.findById(id)
            .orElseThrow(SectionAnnouncementNotFoundException::new);
    }

    // publishedAt이 미래(예약 게시)인 것은 그 시각이 되기 전까지 목록에서 제외한다.
    public List<SectionAnnouncement> getAnnouncements(Long sectionId) {
        return sectionAnnouncementRepository.findAllPublishedBySectionId(sectionId, LocalDateTime.now());
    }

    public List<SectionAnnouncement> getAnnouncementsToNotify(LocalDateTime now) {
        return sectionAnnouncementRepository.findAllToNotify(now);
    }
}
