package kgu.developers.api.teammessage.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SectionTeamMessagePersistResponse(
    @Schema(description = "메시지를 등록한 팀 수", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    int sentCount
) {
}
