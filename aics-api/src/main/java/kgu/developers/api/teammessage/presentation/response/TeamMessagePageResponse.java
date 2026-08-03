package kgu.developers.api.teammessage.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record TeamMessagePageResponse(

    @Schema(description = "메시지 목록", requiredMode = REQUIRED)
    List<TeamMessageResponse> contents,

    @Schema(description = "페이지 정보", requiredMode = REQUIRED)
    PageableResponse<TeamMessageResponse> pageable
) {
    public static TeamMessagePageResponse from(Page<TeamMessage> page) {
        List<TeamMessageResponse> contents = page.getContent().stream()
            .map(TeamMessageResponse::from)
            .toList();

        PageableResponse<TeamMessageResponse> pageable = PageableResponse.<TeamMessageResponse>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements((int) page.getTotalElements())
            .isEnd(page.isLast())
            .build();

        return TeamMessagePageResponse.builder()
            .contents(contents)
            .pageable(pageable)
            .build();
    }
}
