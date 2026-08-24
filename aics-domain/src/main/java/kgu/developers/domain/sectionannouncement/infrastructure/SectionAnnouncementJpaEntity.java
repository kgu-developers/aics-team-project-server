package kgu.developers.domain.sectionannouncement.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"section_announcement\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SectionAnnouncementJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Version
    private long version;

    public SectionAnnouncement toDomain() {
        return SectionAnnouncement.builder()
            .id(this.id)
            .sectionId(this.sectionId)
            .title(this.title)
            .content(this.content)
            .publishedAt(this.publishedAt)
            .version(this.version)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static SectionAnnouncementJpaEntity toEntity(SectionAnnouncement domain) {
        return SectionAnnouncementJpaEntity.builder()
            .id(domain.getId())
            .sectionId(domain.getSectionId())
            .title(domain.getTitle())
            .content(domain.getContent())
            .publishedAt(domain.getPublishedAt())
            .version(domain.getVersion())
            .build();
    }
}
