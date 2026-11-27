package kgu.developers.api.announcementpopup.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AnnouncementPopupUpdateRequest(

    @Schema(description = "팝업 제목(보내면 교체). 빈 문자열이나 공백을 보내면 제목을 지운다. 보내지 않으면 유지", example = "수정된 제목")
    @Size(max = 200)
    String title,

    @Schema(description = "팝업 내용(보내면 교체)", example = "수정된 안내 문구")
    String content,

    @Schema(description = "노출 여부. false면 학생에게 보이지 않는다", example = "false")
    Boolean enabled,

    @Schema(description = "true면 기존 이미지를 제거한다. 새 이미지를 함께 보낼 수 없다", example = "false")
    Boolean removeImage
) {
}
