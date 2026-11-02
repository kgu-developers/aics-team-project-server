package kgu.developers.admin.team.application;

import static kgu.developers.domain.teamMember.domain.TeamMemberWithUser.mapAll;

import java.util.List;

import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamAdminFacade {
	private final TeamQueryService teamQueryService;
	private final TeamCommandService teamCommandService;
	private final TeamMemberQueryService teamMemberQueryService;

	public TeamAdminDetailResponse getTeamById(Long teamId) {
		return TeamAdminDetailResponse.of(teamQueryService.getTeamById(teamId), members(teamId));
	}

	public TeamAdminListResponse finalizeTeams(Long sectionId, String actorId) {
		return toListResponse(teamCommandService.finalizeTeams(sectionId, actorId));
	}

	public TeamAdminListResponse unfinalizeTeams(Long sectionId, String actorId, String reason) {
		return toListResponse(teamCommandService.unfinalizeTeams(sectionId, actorId, reason));
	}

	private TeamAdminListResponse toListResponse(List<Team> teams) {
		List<Long> teamIds = teams.stream().map(Team::getId).toList();
		return TeamAdminListResponse.from(teams, teamMemberQueryService.getLeaderNamesByTeamIds(teamIds));
	}

	private List<TeamMemberAdminResponse> members(Long teamId) {
		return mapAll(teamMemberQueryService.getTeamMembersWithUsers(teamId), TeamMemberAdminResponse::of);
	}
}
