package kgu.developers.domain.sectionannouncement.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SectionAnnouncementAttachment {

    private Long id;
    private Long announcementId;
    private Long fileId;
    private LocalDateTime createdAt;

    public static SectionAnnouncementAttachment create(Long announcementId, Long fileId) {
        return SectionAnnouncementAttachment.builder()
            .announcementId(announcementId)
            .fileId(fileId)
            .build();
    }
}
