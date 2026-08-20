package kgu.developers.admin.team.application;

import java.util.List;

import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
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

	public TeamAdminListResponse finalizeTeams(Long sectionId) {
		return TeamAdminListResponse.from(teamCommandService.finalizeTeams(sectionId));
	}

	private List<TeamMemberAdminResponse> members(Long teamId) {
		return teamMemberQueryService.getTeamMembersWithUsers(teamId).stream()
			.map(it -> TeamMemberAdminResponse.of(it.member(), it.user()))
			.toList();
	}
}
