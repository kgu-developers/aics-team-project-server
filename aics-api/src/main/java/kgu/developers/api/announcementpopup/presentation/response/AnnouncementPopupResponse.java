package kgu.developers.api.announcementpopup.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import lombok.Builder;

@Builder
public record AnnouncementPopupResponse(

    @Schema(description = "팝업 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "부모 공지사항 식별자(이 공지를 팝업으로 띄운다)", example = "3", requiredMode = REQUIRED)
    Long announcementId,

    @Schema(description = "분반 식별자", example = "1", requiredMode = REQUIRED)
    Long sectionId,

    @Schema(description = "팝업 제목. 제목 없이 등록했으면 null")
    String title,

    @Schema(description = "팝업 내용", requiredMode = REQUIRED)
    String content,

    @Schema(description = "이미지 임시 URL(15분 후 만료). 이미지가 없으면 null")
    String imageUrl,

    @Schema(description = "노출 여부", example = "true", requiredMode = REQUIRED)
    boolean enabled,

    @Schema(description = "수정할 때마다 오르는 값. 학생이 '일주일간 보지 않기'를 누른 뒤 팝업이 바뀌었는지 판단하는 기준", example = "0", requiredMode = REQUIRED)
    long version,

    @Schema(description = "생성 일시", requiredMode = REQUIRED)
    LocalDateTime createdAt,

    @Schema(description = "수정 일시", requiredMode = REQUIRED)
    LocalDateTime updatedAt
) {

    public static AnnouncementPopupResponse from(AnnouncementPopup popup, String imageUrl) {
        return AnnouncementPopupResponse.builder()
            .id(popup.getId())
            .announcementId(popup.getAnnouncementId())
            .sectionId(popup.getSectionId())
            .title(popup.getTitle())
            .content(popup.getContent())
            .imageUrl(imageUrl)
            .enabled(popup.isEnabled())
            .version(popup.getVersion())
            .createdAt(popup.getCreatedAt())
            .updatedAt(popup.getUpdatedAt())
            .build();
    }
}
