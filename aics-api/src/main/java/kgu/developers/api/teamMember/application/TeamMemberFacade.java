package kgu.developers.api.teamMember.application;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactResponse;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamMemberFacade {
	private final TeamQueryService teamQueryService;
	private final TeamMemberQueryService teamMemberQueryService;
	private final UserQueryService userQueryService;

	public TeamMemberContactListResponse getContacts(Long teamId) {
		teamQueryService.validateContactVisible(teamId);

		List<TeamMember> teamMembers = teamMemberQueryService.getTeamMembersByTeamId(teamId);
		Map<String, User> users = userQueryService
			.getUsersByStudentNumbers(teamMembers.stream().map(TeamMember::getUserId).toList())
			.stream()
			.collect(toMap(User::getStudentNumber, identity()));

		return new TeamMemberContactListResponse(teamMembers.stream()
			.map(member -> TeamMemberContactResponse.of(member, users.get(member.getUserId())))
			.toList());
	}
}
