package kgu.developers.domain.sectionannouncement.infrastructure;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"section_announcement_attachment\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SectionAnnouncementAttachmentJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    public SectionAnnouncementAttachment toDomain() {
        return SectionAnnouncementAttachment.builder()
            .id(this.id)
            .announcementId(this.announcementId)
            .fileId(this.fileId)
            .createdAt(this.getCreatedAt())
            .build();
    }

    public static SectionAnnouncementAttachmentJpaEntity toEntity(SectionAnnouncementAttachment domain) {
        return SectionAnnouncementAttachmentJpaEntity.builder()
            .id(domain.getId())
            .announcementId(domain.getAnnouncementId())
            .fileId(domain.getFileId())
            .build();
    }
}
