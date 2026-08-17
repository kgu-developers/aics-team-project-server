package kgu.developers.admin.team.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.team.domain.Team;
import lombok.Builder;

@Builder
public record TeamAdminListResponse(
    @Schema(description = "팀 리스트", requiredMode = REQUIRED)
    List<TeamAdminResponse> contents
) {
    public static TeamAdminListResponse from(List<Team> teams) {
        return TeamAdminListResponse.builder()
                .contents(teams.stream().map(TeamAdminResponse::from).toList())
                .build();
    }
}
