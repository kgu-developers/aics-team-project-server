package kgu.developers.api.announcementpopup.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record StudentAnnouncementPopupListResponse(
    @Schema(description = "활성 학생의 분반에서 게시·노출 중이며 숨기지 않은 팝업. 최신순, 없으면 빈 배열")
    List<AnnouncementPopupResponse> contents
) {
}
