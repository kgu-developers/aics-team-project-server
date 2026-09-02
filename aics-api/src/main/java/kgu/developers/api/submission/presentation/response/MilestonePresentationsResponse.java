package kgu.developers.api.submission.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MilestonePresentationsResponse(

        @Schema(description = "발표순서로 정렬된 팀별 공개자료 목록(이전/다음 네비게이션은 이 배열로 프론트에서 처리)", requiredMode = REQUIRED)
        List<TeamPresentationResponse> contents
) {
}
