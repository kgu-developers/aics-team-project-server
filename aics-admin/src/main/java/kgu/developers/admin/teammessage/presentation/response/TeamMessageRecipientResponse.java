package kgu.developers.admin.teammessage.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.team.domain.Team;
import lombok.Builder;

@Builder
public record TeamMessageRecipientResponse(

    @Schema(description = "팀 식별자", example = "20", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "A팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "팀장 이름(팀장이 없으면 null)", example = "홍길동", requiredMode = NOT_REQUIRED)
    String leaderName
) {
    public static TeamMessageRecipientResponse from(Team team, String leaderName) {
        return TeamMessageRecipientResponse.builder()
            .teamId(team.getId())
            .teamName(team.getName())
            .leaderName(leaderName)
            .build();
    }
}
