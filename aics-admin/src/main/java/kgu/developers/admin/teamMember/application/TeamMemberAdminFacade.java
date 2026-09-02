package kgu.developers.admin.teamMember.application;

import static kgu.developers.domain.auditLog.domain.AuditLogEventType.TEAM_UPDATED;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
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
public class TeamMemberAdminFacade {
  private final TeamMemberQueryService teamMemberQueryService;
  private final TeamMemberCommandService teamMemberCommandService;
  private final UserQueryService userQueryService;
  private final TeamQueryService teamQueryService;
  private final AuditLogCommandService auditLogCommandService;

  @Transactional
  public TeamMemberAdminResponse updateTeamMember(
      Long teamId, String studentNumber, TeamMemberUpdateRequest request, String actorId) {
    TeamMember teamMember = teamMemberQueryService.getTeamMember(teamId, studentNumber);
    Team sourceTeam = teamQueryService.getTeamById(teamId);
    List<TeamMemberSnapshot> sourceBefore = memberSnapshots(
        teamMemberQueryService.getTeamMembersByTeamId(teamId));
    Team targetTeam = targetTeam(sourceTeam, request.targetTeamId());
    List<TeamMemberSnapshot> targetBefore = targetTeam == null
        ? List.of()
        : memberSnapshots(teamMemberQueryService.getTeamMembersByTeamId(targetTeam.getId()));

    TeamMember updated = teamMemberCommandService.updateTeamMember(
        teamMember, request.targetTeamId(), request.projectRole(), request.isLeader());
    recordChanges(actorId, sourceTeam, studentNumber, sourceBefore,
        memberSnapshots(teamMemberQueryService.getTeamMembersByTeamId(sourceTeam.getId())));
    if (targetTeam != null) {
      recordChanges(actorId, targetTeam, studentNumber, targetBefore,
          memberSnapshots(teamMemberQueryService.getTeamMembersByTeamId(targetTeam.getId())));
    }
    User user = userQueryService.getUsersByStudentNumbers(List.of(studentNumber)).stream()
        .findFirst()
        .orElse(null);
    return TeamMemberAdminResponse.of(updated, user);
  }

  private Team targetTeam(Team sourceTeam, Long targetTeamId) {
    if (targetTeamId == null || Objects.equals(sourceTeam.getId(), targetTeamId)) {
      return null;
    }
    return teamQueryService.getTeamById(targetTeamId);
  }

  private void recordChanges(
      String actorId,
      Team team,
      String affectedStudentNumber,
      List<TeamMemberSnapshot> before,
      List<TeamMemberSnapshot> after
  ) {
    if (before.equals(after)) {
      return;
    }

    Map<String, Object> metadata = Map.of(
        "changeType", "TEAM_MEMBER_UPDATED",
        "affectedStudentNumber", affectedStudentNumber,
        "before", new TeamMembersSnapshot(before),
        "after", new TeamMembersSnapshot(after)
    );
    record(actorId, team, metadata);
  }

  private void record(String actorId, Team team, Map<String, Object> metadata) {
    auditLogCommandService.recordTeamChange(
        actorId,
        team.getSectionId(),
        team.getId(),
        TEAM_UPDATED,
        JsonConverter.toTree(metadata)
    );
  }

  private List<TeamMemberSnapshot> memberSnapshots(List<TeamMember> members) {
    return members.stream()
        .map(TeamMemberSnapshot::from)
        .sorted(Comparator.comparing(TeamMemberSnapshot::studentNumber))
        .toList();
  }

  private record TeamMembersSnapshot(List<TeamMemberSnapshot> members) {
  }

  private record TeamMemberSnapshot(String studentNumber, boolean leader, String projectRole) {
    private static TeamMemberSnapshot from(TeamMember member) {
      return new TeamMemberSnapshot(
          member.getUserId(), member.isLeader(), member.getProjectRole());
    }
  }
}
