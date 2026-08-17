package kgu.developers.admin.team.application;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamAdminFacade {
	private final TeamQueryService teamQueryService;
	private final TeamCommandService teamCommandService;
	private final TeamMemberQueryService teamMemberQueryService;
	private final UserQueryService userQueryService;

	public TeamAdminDetailResponse getTeamById(Long teamId) {
		Team team = teamQueryService.getTeamById(teamId);
		List<TeamMember> teamMembers = teamMemberQueryService.getTeamMembersByTeamId(teamId);

		Map<String, User> users = userQueryService
			.getUsersByStudentNumbers(teamMembers.stream().map(TeamMember::getUserId).toList())
			.stream()
			.collect(toMap(User::getStudentNumber, identity()));

		return TeamAdminDetailResponse.of(team, teamMembers.stream()
			.map(member -> TeamMemberAdminResponse.of(member, users.get(member.getUserId())))
			.toList());
	}

	public TeamAdminListResponse finalizeTeams(Long sectionId) {
		return TeamAdminListResponse.from(teamCommandService.finalizeTeams(sectionId));
	}
}
