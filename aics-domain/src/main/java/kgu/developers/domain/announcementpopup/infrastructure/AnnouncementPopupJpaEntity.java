package kgu.developers.domain.announcementpopup.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "announcement_popup",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_announcement_popup_announcement", columnNames = "announcement_id"),
    indexes = @Index(name = "idx_announcement_popup_section", columnList = "section_id")
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class AnnouncementPopupJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "image_file_id")
    private Long imageFileId;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    private long version;

    public AnnouncementPopup toDomain() {
        return AnnouncementPopup.builder()
            .id(this.id)
            .announcementId(this.announcementId)
            .sectionId(this.sectionId)
            .title(this.title)
            .content(this.content)
            .imageFileId(this.imageFileId)
            .enabled(this.enabled)
            .version(this.version)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static AnnouncementPopupJpaEntity toEntity(AnnouncementPopup domain) {
        return AnnouncementPopupJpaEntity.builder()
            .id(domain.getId())
            .announcementId(domain.getAnnouncementId())
            .sectionId(domain.getSectionId())
            .title(domain.getTitle())
            .content(domain.getContent())
            .imageFileId(domain.getImageFileId())
            .enabled(domain.isEnabled())
            .version(domain.getVersion())
            .build();
    }
}
