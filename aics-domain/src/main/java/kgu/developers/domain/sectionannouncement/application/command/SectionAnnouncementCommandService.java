package kgu.developers.domain.sectionannouncement.application.command;

import java.time.LocalDateTime;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementRepository;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementEmptyUpdateException;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementInvalidContentException;
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
        if (title == null && content == null && publishedAt == null) {
            throw new SectionAnnouncementEmptyUpdateException();
        }

        SectionAnnouncement announcement = findOrThrow(id);

        if (title != null) {
            if (title.isBlank()) {
                throw new SectionAnnouncementInvalidContentException();
            }
            announcement.updateTitle(title);
        }
        if (content != null) {
            if (content.isBlank()) {
                throw new SectionAnnouncementInvalidContentException();
            }
            announcement.updateContent(content);
        }
        if (publishedAt != null) {
            announcement.updatePublishedAt(publishedAt);
        }

        sectionAnnouncementRepository.save(announcement);
    }

    public void markNotified(Long id, LocalDateTime notifiedAt) {
        SectionAnnouncement announcement = findOrThrow(id);
        announcement.markNotified(notifiedAt);
        sectionAnnouncementRepository.save(announcement);
    }

    private SectionAnnouncement findOrThrow(Long id) {
        return sectionAnnouncementRepository.findById(id)
            .orElseThrow(SectionAnnouncementNotFoundException::new);
    }
}
