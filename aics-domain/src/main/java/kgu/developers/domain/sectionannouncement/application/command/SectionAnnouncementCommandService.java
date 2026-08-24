package kgu.developers.domain.sectionannouncement.application.command;

import java.time.LocalDateTime;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementRepository;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SectionAnnouncementCommandService {

    private final SectionAnnouncementRepository sectionAnnouncementRepository;

    public Long createAnnouncement(Long sectionId, String title, String content, LocalDateTime publishedAt) {
        SectionAnnouncement announcement = SectionAnnouncement.create(sectionId, title, content, publishedAt);
        return sectionAnnouncementRepository.save(announcement).getId();
    }

    public void updateAnnouncement(Long id, String title, String content, LocalDateTime publishedAt) {
        SectionAnnouncement announcement = findOrThrow(id);

        if (title != null) {
            announcement.updateTitle(title);
        }
        if (content != null) {
            announcement.updateContent(content);
        }
        if (publishedAt != null) {
            announcement.updatePublishedAt(publishedAt);
        }

        sectionAnnouncementRepository.save(announcement);
    }

    private SectionAnnouncement findOrThrow(Long id) {
        return sectionAnnouncementRepository.findById(id)
            .orElseThrow(SectionAnnouncementNotFoundException::new);
    }
}
