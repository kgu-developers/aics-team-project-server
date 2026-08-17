package kgu.developers.admin.team.application;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import kgu.developers.admin.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.admin.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminKickoffResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
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
public class TeamAdminFacade {
	private final TeamQueryService teamQueryService;
	private final TeamCommandService teamCommandService;
	private final TeamMemberQueryService teamMemberQueryService;
	private final TeamMemberCommandService teamMemberCommandService;
	private final UserQueryService userQueryService;

	public TeamAdminDetailResponse getTeamById(Long teamId) {
		return TeamAdminDetailResponse.of(teamQueryService.getTeamById(teamId), members(teamId));
	}

	public TeamAdminListResponse finalizeTeams(Long sectionId) {
		return TeamAdminListResponse.from(teamCommandService.finalizeTeams(sectionId));
	}

	public TeamAdminKickoffResponse getKickoffByTeamId(Long teamId) {
		return TeamAdminKickoffResponse.of(teamQueryService.getTeamById(teamId), members(teamId));
	}

	// 팀 정보 저장과 역할분담 저장이 따로 커밋되면 절반만 반영된다
	@Transactional
	public TeamAdminKickoffResponse updateKickoff(Long teamId, TeamKickoffUpdateRequest request) {
		Team team = teamCommandService.updateKickoff(
			teamId, request.name(), request.topic(), request.kickoffRule(), request.meetingSchedule());

		// 학번이 겹치면 마지막 값을 쓴다
		Map<String, String> projectRoles = request.memberRoles() == null ? Map.of()
			: request.memberRoles().stream()
			.collect(toMap(MemberRole::studentNumber, MemberRole::projectRole, (before, after) -> after));
		teamMemberCommandService.updateKickoffRoles(teamId, request.leaderStudentNumber(), projectRoles);

		return TeamAdminKickoffResponse.of(team, members(teamId));
	}

	private List<TeamMemberAdminResponse> members(Long teamId) {
		List<TeamMember> teamMembers = teamMemberQueryService.getTeamMembersByTeamId(teamId);

		Map<String, User> users = userQueryService
			.getUsersByStudentNumbers(teamMembers.stream().map(TeamMember::getUserId).toList())
			.stream()
			.collect(toMap(User::getStudentNumber, identity()));

		return teamMembers.stream()
			.map(member -> TeamMemberAdminResponse.of(member, users.get(member.getUserId())))
			.toList();
	}
}
