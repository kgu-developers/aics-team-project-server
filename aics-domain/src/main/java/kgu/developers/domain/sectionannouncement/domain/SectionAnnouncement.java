package kgu.developers.domain.sectionannouncement.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SectionAnnouncement {

    private Long id;
    private Long sectionId;
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SectionAnnouncement create(Long sectionId, String title, String content, LocalDateTime publishedAt) {
        return SectionAnnouncement.builder()
            .sectionId(sectionId)
            .title(title)
            .content(content)
            .publishedAt(publishedAt)
            .build();
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updatePublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
