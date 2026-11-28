package kgu.developers.api.announcementpopup.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AnnouncementPopupDismissalResponse(
    Long popupId,
    @Schema(description = "학생 숨김 기준인 팝업 내용 버전")
    long contentVersion,
    @Schema(description = "KST 기준 숨김 만료 시각. 이 시각부터 재노출되며, 팝업 내용 변경 시 기한 전에도 재노출")
    LocalDateTime hiddenUntil
) {
}
