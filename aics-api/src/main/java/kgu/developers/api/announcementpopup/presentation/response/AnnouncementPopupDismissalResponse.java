package kgu.developers.api.announcementpopup.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AnnouncementPopupDismissalResponse(
    Long popupId,
    long version,
    @Schema(description = "KST 기준 숨김 만료 시각. 이 시각부터 재노출되며, 팝업 수정 시 기한 전에도 재노출")
    LocalDateTime hiddenUntil
) {
}
