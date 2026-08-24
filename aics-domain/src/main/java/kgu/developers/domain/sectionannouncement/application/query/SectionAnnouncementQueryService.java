package kgu.developers.domain.sectionannouncement.application.query;

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

    public List<SectionAnnouncement> getAnnouncements(Long sectionId) {
        return sectionAnnouncementRepository.findAllBySectionId(sectionId);
    }
}
