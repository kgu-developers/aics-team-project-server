package kgu.developers.api.team.application;

import static kgu.developers.domain.teamMember.domain.TeamMemberWithUser.mapAll;

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
import kgu.developers.domain.teamMember.domain.TeamMember;
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
    List<TeamMember> updatedMembers =
        teamMemberCommandService.updateKickoffRoles(teamId, request.leaderStudentNumber(), projectRoles);

    return TeamKickoffResponse.of(team,
        mapAll(teamMemberQueryService.withUsers(updatedMembers), TeamMemberResponse::of));
  }

  @Transactional
  public void claimLeader(Long teamId, String userId) {
    teamAccessValidator.validateMembership(teamId, userId);
    teamMemberCommandService.claimLeader(teamId, userId);
  }

  private List<TeamMemberResponse> members(Long teamId) {
    return mapAll(teamMemberQueryService.getTeamMembersWithUsers(teamId), TeamMemberResponse::of);
  }
}
