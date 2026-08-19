package kgu.developers.api.team.application;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TeamFacade {
	private final TeamQueryService teamQueryService;
	private final TeamCommandService teamCommandService;
	private final TeamMemberQueryService teamMemberQueryService;
	private final TeamMemberCommandService teamMemberCommandService;
	private final UserQueryService userQueryService;

	public TeamKickoffResponse getKickoffByTeamId(Long teamId) {
		return TeamKickoffResponse.of(teamQueryService.getTeamById(teamId), members(teamId));
	}

	@Transactional
	public TeamKickoffResponse updateKickoff(Long teamId, TeamKickoffUpdateRequest request) {
		Team team = teamCommandService.updateKickoff(
			teamId, request.name(), request.kickoffRule(), request.meetingSchedule());

		// 학번이 겹치면 마지막 값을 쓴다
		Map<String, String> projectRoles = request.memberRoles() == null ? Map.of()
			: request.memberRoles().stream()
			.collect(toMap(MemberRole::studentNumber, MemberRole::projectRole, (before, after) -> after));
		teamMemberCommandService.updateKickoffRoles(teamId, request.leaderStudentNumber(), projectRoles);

		return TeamKickoffResponse.of(team, members(teamId));
	}

	private List<TeamMemberResponse> members(Long teamId) {
		List<TeamMember> teamMembers = teamMemberQueryService.getTeamMembersByTeamId(teamId);

		Map<String, User> users = userQueryService
			.getUsersByStudentNumbers(teamMembers.stream().map(TeamMember::getUserId).toList())
			.stream()
			.collect(toMap(User::getStudentNumber, identity()));

		return teamMembers.stream()
			.map(member -> TeamMemberResponse.of(member, users.get(member.getUserId())))
			.toList();
	}
}
