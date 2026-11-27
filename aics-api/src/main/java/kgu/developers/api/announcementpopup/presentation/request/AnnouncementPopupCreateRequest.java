package kgu.developers.api.announcementpopup.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AnnouncementPopupCreateRequest(

    @Schema(description = "팝업 제목(선택). 비워 두거나 공백이면 제목 없이 등록된다", example = "중간보고서 안내")
    @Size(max = 200)
    String title,

    @Schema(description = "팝업 내용", example = "중간보고서 피드백 반영 방향을 11/20까지 제출해주세요.", requiredMode = REQUIRED)
    @NotBlank
    String content
) {
}
