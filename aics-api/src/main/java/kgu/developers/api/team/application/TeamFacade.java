package kgu.developers.api.team.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
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
  private final TeamAccessValidator teamAccessValidator;

  public TeamKickoffResponse getKickoffByTeamId(Long teamId, String userId) {
    teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
    return TeamKickoffResponse.of(teamQueryService.getTeamById(teamId), members(teamId));
  }

  @Transactional
  public TeamKickoffResponse updateKickoff(Long teamId, String userId, TeamKickoffUpdateRequest request) {
    teamAccessValidator.validateMembership(teamId, userId);

    Team team = teamCommandService.updateKickoff(
        teamId, request.name(), request.kickoffRule(), request.meetingSchedule());

    Map<String, String> projectRoles = new HashMap<>();
    if (request.memberRoles() != null) {
      request.memberRoles().forEach(role -> projectRoles.put(role.studentNumber(), role.projectRole()));
    }
    teamMemberCommandService.updateKickoffRoles(teamId, request.leaderStudentNumber(), projectRoles);

    return TeamKickoffResponse.of(team, members(teamId));
  }

  private List<TeamMemberResponse> members(Long teamId) {
    return teamMemberQueryService.getTeamMembersWithUsers(teamId).stream()
        .map(it -> TeamMemberResponse.of(it.member(), it.user()))
        .toList();
  }
}
