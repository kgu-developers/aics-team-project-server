package kgu.developers.domain.announcementpopup.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 공지사항을 학생 화면에 팝업으로 띄우기 위한 안내. 팝업은 부모 공지({@code announcementId})와 별개로
 * 교수가 직접 쓴 내용과 이미지를 가지며, 공지 한 건당 하나만 둔다.
 *
 * <p>노출 기간은 두지 않는다. 켜져 있는 동안 분반 학생 전체에게 보이고, 교수가 끄면 보이지 않는다.
 *
 * <p>{@code version}은 JPA 낙관적 락 값이자 "팝업이 수정되었는가"의 기준이다. 값은 저장할 때마다 오르며
 * 내용이 같아도 오른다. 공지와 달리 팝업에는 알림 발송처럼 교수의 수정과 무관하게 저장하는 경로가 없으므로,
 * 값이 오르는 것은 교수가 등록·수정했을 때뿐이다. 학생이 "일주일간 보지 않기"를 누른 시점의 version이
 * 이 값보다 작으면 다시 보여준다. 팝업을 저장하는 새 경로(배치 등)를 추가하면 이 전제가 깨지므로 주의한다.
 */
@Getter
@Builder
@AllArgsConstructor
public class AnnouncementPopup {

    private Long id;
    private Long announcementId;
    private Long sectionId;
    private String title;
    private String content;
    private Long imageFileId;
    private boolean enabled;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 제목은 선택이다. 비어 있거나 공백이면 제목 없음(null)으로 저장한다. */
    public static AnnouncementPopup create(
        Long announcementId, Long sectionId, String title, String content, Long imageFileId
    ) {
        return AnnouncementPopup.builder()
            .announcementId(announcementId)
            .sectionId(sectionId)
            .title(normalizeTitle(title))
            .content(content)
            .imageFileId(imageFileId)
            .enabled(true)
            .build();
    }

    /** 빈 문자열이나 공백을 보내면 제목을 지운다(null). */
    public void updateTitle(String title) {
        this.title = normalizeTitle(title);
    }

    public boolean hasTitle() {
        return this.title != null;
    }

    private static String normalizeTitle(String title) {
        return title == null || title.isBlank() ? null : title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void changeImage(Long imageFileId) {
        this.imageFileId = imageFileId;
    }

    public void removeImage() {
        this.imageFileId = null;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasImage() {
        return this.imageFileId != null;
    }
}
