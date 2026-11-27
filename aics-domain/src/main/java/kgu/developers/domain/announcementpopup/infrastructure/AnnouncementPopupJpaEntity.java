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

    // 학생에게 보이는 내용(제목·내용·이미지)이 바뀔 때만 오른다. 학생 숨김의 기준(AnnouncementPopup 참고).
    @Column(name = "content_version", nullable = false)
    private long contentVersion;

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
            .contentVersion(this.contentVersion)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static AnnouncementPopupJpaEntity toEntity(AnnouncementPopup domain) {
        AnnouncementPopupJpaEntity entity = AnnouncementPopupJpaEntity.builder()
            .id(domain.getId())
            .announcementId(domain.getAnnouncementId())
            .sectionId(domain.getSectionId())
            .title(domain.getTitle())
            .content(domain.getContent())
            .imageFileId(domain.getImageFileId())
            .enabled(domain.isEnabled())
            .version(domain.getVersion())
            .contentVersion(domain.getContentVersion())
            .build();
        // createdAt은 @Column(updatable = false)라 UPDATE SQL에는 들어가지 않지만, 여기서 채우지 않으면
        // merge()가 null을 이미 영속 상태인 엔티티에 복사해, 같은 트랜잭션에서 바로 다시 읽은 도메인 객체의
        // createdAt이 null로 남는다. AnnouncementPopupFacade.updatePopup()이 수정 직후 같은 트랜잭션에서
        // 다시 읽어 응답을 만들기 때문에 수정 응답의 createdAt이 null로 나갔다(MeetingActionJpaEntity와 같은 문제).
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
}
