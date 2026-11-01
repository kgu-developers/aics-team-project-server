package kgu.developers.api.teammessage.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SectionTeamMessageCreateRequest(
    @Schema(description = "분반 전체 팀에 보낼 메시지 본문", example = "다음 주 발표 일정을 확인해주세요.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String message,
    @Schema(description = "전송 대상 팀 id 목록. 생략하거나 비우면 분반 전체 팀에 전송한다.", example = "[1, 2, 3]")
    List<Long> teamIds
) {
}
