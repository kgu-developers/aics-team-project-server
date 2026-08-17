package kgu.developers.admin.teamMember.application;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberContactAdminListResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberContactAdminResponse;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamMemberAdminFacade {
	private final TeamMemberQueryService teamMemberQueryService;
	private final TeamQueryService teamQueryService;
	private final TeamMemberCommandService teamMemberCommandService;
	private final UserQueryService userQueryService;

	public TeamMemberAdminResponse updateTeamMember(
		Long teamId, String studentNumber, TeamMemberUpdateRequest request) {
		TeamMember teamMember = teamMemberQueryService.getTeamMember(teamId, studentNumber);

		TeamMember updated = teamMemberCommandService.updateTeamMember(
			teamMember, request.targetTeamId(), request.projectRole(), request.isLeader());

		return TeamMemberAdminResponse.of(updated, userQueryService.getUserByStudentNumber(studentNumber));
	}

	public TeamMemberContactAdminListResponse getContacts(Long teamId) {
		teamQueryService.validateContactVisible(teamId);

		List<TeamMember> teamMembers = teamMemberQueryService.getTeamMembersByTeamId(teamId);
		Map<String, User> users = userQueryService
			.getUsersByStudentNumbers(teamMembers.stream().map(TeamMember::getUserId).toList())
			.stream()
			.collect(toMap(User::getStudentNumber, identity()));

		return new TeamMemberContactAdminListResponse(teamMembers.stream()
			.map(member -> TeamMemberContactAdminResponse.of(member, users.get(member.getUserId())))
			.toList());
	}
}
