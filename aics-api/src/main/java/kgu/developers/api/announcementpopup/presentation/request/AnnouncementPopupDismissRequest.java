package kgu.developers.api.announcementpopup.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AnnouncementPopupDismissRequest(
    @Schema(description = "학생이 조회한 팝업의 version. 수정된 팝업을 보지 않고 숨기는 것을 방지한다", example = "0")
    @NotNull @PositiveOrZero Long version
) {
}
