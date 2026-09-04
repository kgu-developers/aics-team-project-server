package kgu.developers.admin.teamMember.application;

import static kgu.developers.domain.auditLog.domain.AuditLogEventType.TEAM_UPDATED;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.auditLog.domain.TeamMembersAuditSnapshot;
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
    Team sourceTeam;
    Team targetTeam = null;
    Long targetTeamId = request.targetTeamId();
    if (targetTeamId != null && !Objects.equals(teamId, targetTeamId)) {
      Long firstId = Math.min(teamId, targetTeamId);
      Long secondId = Math.max(teamId, targetTeamId);
      Team first = teamQueryService.getTeamByIdForUpdate(firstId);
      Team second = teamQueryService.getTeamByIdForUpdate(secondId);
      sourceTeam = Objects.equals(first.getId(), teamId) ? first : second;
      targetTeam = Objects.equals(first.getId(), targetTeamId) ? first : second;
    } else {
      sourceTeam = teamQueryService.getTeamByIdForUpdate(teamId);
    }

    TeamMember teamMember = teamMemberQueryService.getTeamMember(teamId, studentNumber);
    TeamMembersAuditSnapshot sourceBefore = TeamMembersAuditSnapshot.from(
        teamMemberQueryService.getTeamMembersByTeamId(teamId));
    TeamMembersAuditSnapshot targetBefore = targetTeam == null
        ? TeamMembersAuditSnapshot.from(List.of())
        : TeamMembersAuditSnapshot.from(
            teamMemberQueryService.getTeamMembersByTeamId(targetTeam.getId()));

    TeamMember updated = teamMemberCommandService.updateTeamMember(
        teamMember, request.targetTeamId(), request.projectRole(), request.isLeader());
    recordChanges(actorId, sourceTeam, studentNumber, sourceBefore,
        TeamMembersAuditSnapshot.from(
            teamMemberQueryService.getTeamMembersByTeamId(sourceTeam.getId())));
    if (targetTeam != null) {
      recordChanges(actorId, targetTeam, studentNumber, targetBefore,
          TeamMembersAuditSnapshot.from(
              teamMemberQueryService.getTeamMembersByTeamId(targetTeam.getId())));
    }
    User user = userQueryService.getUsersByStudentNumbers(List.of(studentNumber)).stream()
        .findFirst()
        .orElse(null);
    return TeamMemberAdminResponse.of(updated, user);
  }

  private void recordChanges(
      String actorId,
      Team team,
      String affectedStudentNumber,
      TeamMembersAuditSnapshot before,
      TeamMembersAuditSnapshot after
  ) {
    if (before.equals(after)) {
      return;
    }

    Map<String, Object> metadata = Map.of(
        "changeType", "TEAM_MEMBER_UPDATED",
        "affectedStudentNumber", affectedStudentNumber,
        "before", before,
        "after", after
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

}
