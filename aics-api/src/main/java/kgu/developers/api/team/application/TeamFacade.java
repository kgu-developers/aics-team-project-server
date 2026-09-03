package kgu.developers.api.team.application;

import static kgu.developers.domain.teamMember.domain.TeamMemberWithUser.mapAll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
import kgu.developers.domain.auditLog.domain.TeamMembersAuditSnapshot;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
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
  private final AuditLogCommandService auditLogCommandService;

  public TeamKickoffResponse getKickoffByTeamId(Long teamId, String userId) {
    teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
    return TeamKickoffResponse.of(teamQueryService.getTeamById(teamId), members(teamId));
  }

  @Transactional
  public TeamKickoffResponse updateKickoff(Long teamId, String userId, TeamKickoffUpdateRequest request) {
    teamAccessValidator.validateMembership(teamId, userId);
    TeamSnapshot beforeTeam = TeamSnapshot.from(teamQueryService.getTeamByIdForUpdate(teamId));
    TeamMembersAuditSnapshot beforeMembers = TeamMembersAuditSnapshot.from(
        teamMemberQueryService.getTeamMembersByTeamId(teamId));

    Team team = teamCommandService.updateKickoff(
        teamId, request.name(), request.kickoffRule(), request.meetingSchedule());

    Map<String, String> projectRoles = new HashMap<>();
    if (request.memberRoles() != null) {
      request.memberRoles().forEach(role -> projectRoles.put(role.studentNumber(), role.projectRole()));
    }
    List<TeamMember> updatedMembers =
        teamMemberCommandService.updateKickoffRoles(teamId, request.leaderStudentNumber(), projectRoles);

    recordKickoffChanges(userId, team, beforeTeam, beforeMembers,
        TeamMembersAuditSnapshot.from(updatedMembers));

    return TeamKickoffResponse.of(team,
        mapAll(teamMemberQueryService.withUsers(updatedMembers), TeamMemberResponse::of));
  }

  @Transactional
  public void claimLeader(Long teamId, String userId) {
    teamAccessValidator.validateMembership(teamId, userId);
    Team team = teamQueryService.getTeamByIdForUpdate(teamId);
    Status beforeStatus = team.getStatus();
    TeamMembersAuditSnapshot beforeMembers = TeamMembersAuditSnapshot.from(
        teamMemberQueryService.getTeamMembersByTeamId(teamId));
    teamMemberCommandService.claimLeader(team, userId);
    TeamMembersAuditSnapshot afterMembers = TeamMembersAuditSnapshot.from(
        teamMemberQueryService.getTeamMembersByTeamId(teamId));
    recordTeamMemberChange(userId, team, "LEADER_CLAIMED", beforeMembers, afterMembers);
    recordTeamStatusChange(userId, team, beforeStatus, team.getStatus());
  }

  private List<TeamMemberResponse> members(Long teamId) {
    return mapAll(teamMemberQueryService.getTeamMembersWithUsers(teamId), TeamMemberResponse::of);
  }

  private void recordKickoffChanges(
      String actorId,
      Team team,
      TeamSnapshot beforeTeam,
      TeamMembersAuditSnapshot beforeMembers,
      TeamMembersAuditSnapshot afterMembers
  ) {
    if (!Objects.equals(beforeTeam.name(), team.getName())) {
      record(actorId, team, AuditLogEventType.TEAM_NAME_UPDATED,
          Map.of("before", new TeamNameSnapshot(beforeTeam.name()),
              "after", new TeamNameSnapshot(team.getName())));
    }

    TeamRuleSnapshot beforeRule = new TeamRuleSnapshot(
        beforeTeam.kickoffRule(), beforeTeam.meetingSchedule());
    TeamRuleSnapshot afterRule = new TeamRuleSnapshot(
        team.getKickoffRule(), team.getMeetingSchedule());
    if (!beforeRule.equals(afterRule)) {
      record(actorId, team, AuditLogEventType.TEAM_RULE_UPDATED,
          Map.of("before", beforeRule, "after", afterRule));
    }

    recordTeamMemberChange(actorId, team, "KICKOFF_MEMBERS_UPDATED", beforeMembers, afterMembers);
  }

  private void recordTeamMemberChange(
      String actorId,
      Team team,
      String changeType,
      TeamMembersAuditSnapshot beforeMembers,
      TeamMembersAuditSnapshot afterMembers
  ) {
    if (beforeMembers.equals(afterMembers)) {
      return;
    }
    record(actorId, team, AuditLogEventType.TEAM_UPDATED,
        Map.of(
            "changeType", changeType,
            "before", beforeMembers,
            "after", afterMembers
        ));
  }

  private void recordTeamStatusChange(
      String actorId,
      Team team,
      Status beforeStatus,
      Status afterStatus
  ) {
    if (beforeStatus == afterStatus) {
      return;
    }
    record(actorId, team, AuditLogEventType.TEAM_UPDATED,
        Map.of(
            "changeType", "TEAM_STATUS_UPDATED",
            "before", new TeamStatusSnapshot(beforeStatus),
            "after", new TeamStatusSnapshot(afterStatus)
        ));
  }

  private void record(String actorId, Team team, AuditLogEventType eventType, Object metadata) {
    auditLogCommandService.recordTeamChange(
        actorId,
        team.getSectionId(),
        team.getId(),
        eventType,
        JsonConverter.toTree(metadata)
    );
  }

  private record TeamSnapshot(String name, String kickoffRule, String meetingSchedule) {
    private static TeamSnapshot from(Team team) {
      return new TeamSnapshot(team.getName(), team.getKickoffRule(), team.getMeetingSchedule());
    }
  }

  private record TeamNameSnapshot(String name) {
  }

  private record TeamRuleSnapshot(String kickoffRule, String meetingSchedule) {
  }

  private record TeamStatusSnapshot(Status status) {
  }

}
