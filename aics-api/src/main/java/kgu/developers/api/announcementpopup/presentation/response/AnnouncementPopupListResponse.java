package kgu.developers.api.announcementpopup.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record AnnouncementPopupListResponse(

    @Schema(description = "팝업 목록(꺼진 팝업 포함, 최신순)", requiredMode = REQUIRED)
    List<AnnouncementPopupResponse> contents
) {

    public static AnnouncementPopupListResponse from(List<AnnouncementPopupResponse> contents) {
        return AnnouncementPopupListResponse.builder().contents(contents).build();
    }
}
