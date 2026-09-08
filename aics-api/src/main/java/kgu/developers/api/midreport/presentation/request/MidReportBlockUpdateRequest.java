package kgu.developers.api.midreport.presentation.request;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record MidReportBlockUpdateRequest(
    @Schema(description = "조회 응답에서 받은 중간보고서 버전", example = "3", requiredMode = REQUIRED)
    @NotNull
    @PositiveOrZero
    Long version,

    @Schema(
        description = "고정 필드 배열. key와 value는 필수이며 label/multiline은 서버 정의로 정규화됩니다.",
        example = "[{\"key\":\"title\",\"label\":\"프로젝트 제목\",\"value\":\"CineFlow\"}]",
        requiredMode = REQUIRED
    )
    @NotNull
    JsonNode fields
) {
}
