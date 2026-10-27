package kgu.developers.api.teammessage.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SectionTeamMessageCreateRequest(
    @Schema(description = "분반 전체 팀에 보낼 메시지 본문", example = "다음 주 발표 일정을 확인해주세요.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String message
) {
}
