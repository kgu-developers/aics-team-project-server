package kgu.developers.api.teamthread.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.teamthread.domain.TeamThread;
import lombok.Builder;

@Builder
public record TeamThreadResponse(

    @Schema(description = "스레드 id", example = "1", requiredMode = REQUIRED)
    Long threadId,

    @Schema(description = "팀 id", example = "1", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "스레드 생성일시", example = "2026-08-03 10:00", requiredMode = REQUIRED)
    String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static TeamThreadResponse from(TeamThread teamThread) {
        return TeamThreadResponse.builder()
            .threadId(teamThread.getId())
            .teamId(teamThread.getTeamId())
            .createdAt(teamThread.getCreatedAt() != null ? teamThread.getCreatedAt().format(FORMATTER) : null)
            .build();
    }
}
