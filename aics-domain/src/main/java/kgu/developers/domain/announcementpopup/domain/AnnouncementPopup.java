package kgu.developers.domain.announcementpopup.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 공지사항을 학생 화면에 팝업으로 띄우기 위한 안내. 팝업은 부모 공지({@code announcementId})와 별개로
 * 교수가 직접 쓴 내용과 이미지를 가지며, 공지 한 건당 하나만 둔다.
 *
 * <p>노출 기간은 두지 않는다. 켜져 있는 동안 분반 학생 전체에게 보이고, 교수가 끄면 보이지 않는다.
 *
 * <p>값이 두 개다. 역할을 섞지 않는다.
 * <ul>
 *   <li>{@code version}: JPA 낙관적 락 값. 저장할 때마다 오르며(노출 여부만 바꿔도, 내용이 같아도) 동시 저장 충돌을
 *       막는 데만 쓴다.</li>
 *   <li>{@code contentVersion}: 학생에게 보이는 내용(제목·내용·이미지)이 실제로 바뀌었을 때만 오른다(바뀐 항목마다 1씩). 학생이
 *       "일주일간 보지 않기"를 누른 시점의 값과 다르면 다시 보여주는 기준이다. 노출을 껐다 켜거나 같은 값으로 다시
 *       저장해도 오르지 않는다.</li>
 * </ul>
 * 학생에게 보이는 필드를 새로 추가하면, 그 필드를 바꾸는 메서드에서도 {@link #touchContent()}를 불러야 한다.
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
    private long contentVersion;
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
        String normalized = normalizeTitle(title);
        if (!Objects.equals(this.title, normalized)) {
            this.title = normalized;
            touchContent();
        }
    }

    public boolean hasTitle() {
        return this.title != null;
    }

    private static String normalizeTitle(String title) {
        return title == null || title.isBlank() ? null : title;
    }

    public void updateContent(String content) {
        if (!Objects.equals(this.content, content)) {
            this.content = content;
            touchContent();
        }
    }

    /** 새로 올린 이미지로 교체한다. 업로드마다 새 파일이라 학생에게는 바뀐 내용으로 본다. */
    public void changeImage(Long imageFileId) {
        if (!Objects.equals(this.imageFileId, imageFileId)) {
            this.imageFileId = imageFileId;
            touchContent();
        }
    }

    public void removeImage() {
        if (this.imageFileId != null) {
            this.imageFileId = null;
            touchContent();
        }
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasImage() {
        return this.imageFileId != null;
    }

    // 학생에게 보이는 내용이 바뀌었음을 기록한다(학생의 "일주일간 보지 않기"를 무효로 만든다).
    private void touchContent() {
        this.contentVersion++;
    }
}
