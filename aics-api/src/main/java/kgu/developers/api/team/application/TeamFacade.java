package kgu.developers.api.team.application;

import static kgu.developers.domain.teamMember.domain.TeamMemberWithUser.mapAll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
import kgu.developers.domain.auditLog.domain.TeamMembersAuditSnapshot;
import kgu.developers.domain.project.application.command.ProjectCommandService;
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
  private final ProjectCommandService projectCommandService;

  public TeamKickoffResponse getKickoffByTeamId(Long teamId, String userId) {
    teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
    return TeamKickoffResponse.of(teamQueryService.getTeamById(teamId), members(teamId));
  }

  @Transactional
  public TeamKickoffResponse updateKickoff(Long teamId, String userId, TeamKickoffUpdateRequest request) {
    teamAccessValidator.validateMembership(teamId, userId);
    KickoffResult result = applyKickoff(teamId, userId, request.name(), request.kickoffRule(),
        request.meetingSchedule(), request.leaderStudentNumber(), request.memberRoles());

    return TeamKickoffResponse.of(result.team(),
        mapAll(teamMemberQueryService.withUsers(result.members()), TeamMemberResponse::of));
  }

  /**
   * 제안서 5번(팀 운영방식)에서 같은 내용을 고칠 때 쓴다. 킥오프와 저장소가 같으므로 여기서 고친 값이
   * 킥오프 화면에도 그대로 반영된다. 팀명·팀장은 제안서 항목이 아니라 건드리지 않고, null로 넘긴 항목은 유지한다.
   */
  @Transactional
  public void updateKickoffContent(
      Long teamId,
      String userId,
      String kickoffRule,
      String meetingSchedule,
      List<MemberRole> memberRoles
  ) {
    teamAccessValidator.validateMembership(teamId, userId);
    applyKickoff(teamId, userId, null, kickoffRule, meetingSchedule, null, memberRoles);
  }

  // null로 넘어온 항목은 지금 값을 유지한다(제안서 저장에서 부르는 경로). 킥오프 API는 전부 필수라 해당 없음.
  private KickoffResult applyKickoff(
      Long teamId,
      String actorId,
      String name,
      String kickoffRule,
      String meetingSchedule,
      String leaderStudentNumber,
      List<MemberRole> memberRoles
  ) {
    TeamSnapshot beforeTeam = TeamSnapshot.from(teamQueryService.getTeamByIdForUpdate(teamId));
    List<TeamMember> before = teamMemberQueryService.getTeamMembersByTeamId(teamId);
    TeamMembersAuditSnapshot beforeMembers = TeamMembersAuditSnapshot.from(before);

    Team team = teamCommandService.updateKickoff(
        teamId,
        name != null ? name : beforeTeam.name(),
        kickoffRule != null ? kickoffRule : beforeTeam.kickoffRule(),
        meetingSchedule != null ? meetingSchedule : beforeTeam.meetingSchedule());

    // 역할분담도 팀장도 넘기지 않았으면 팀원은 손대지 않는다(회의방식·팀규칙만 고치는 경우).
    List<TeamMember> updatedMembers = before;
    if (leaderStudentNumber != null || memberRoles != null) {
      Map<String, String> projectRoles = new HashMap<>();
      if (memberRoles != null) {
        memberRoles.forEach(role -> projectRoles.put(role.studentNumber(), role.projectRole()));
      }
      updatedMembers = teamMemberCommandService.updateKickoffRoles(teamId,
          leaderStudentNumber != null ? leaderStudentNumber : currentLeaderId(before), projectRoles);
    }

    TeamMembersAuditSnapshot afterMembers = TeamMembersAuditSnapshot.from(updatedMembers);
    recordKickoffChanges(actorId, team, beforeTeam, beforeMembers, afterMembers);

    // 제안서 5번(팀 운영방식)은 이 킥오프 정보를 그대로 보여준다. 내용이 실제로 바뀌었으면
    // 제안서가 바뀐 것이므로 이전 리비전의 동의를 무효화한다.
    if (kickoffContentChanged(beforeTeam, team, beforeMembers, afterMembers)) {
      projectCommandService.invalidateProposalForKickoffChange(teamId);
    }

    return new KickoffResult(team, updatedMembers);
  }

  private String currentLeaderId(List<TeamMember> members) {
    return members.stream()
        .filter(TeamMember::isLeader)
        .findFirst()
        .map(TeamMember::getUserId)
        .orElse(null);  // 팀장이 없으면 updateKickoffRoles가 TeamMemberNotFoundException으로 걸러낸다
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

  // 팀명은 제안서 5번에 들어가지 않으므로 이름만 바뀐 경우는 제외한다.
  // 팀장 여부도 제안서 항목이 아니므로 제외하고 역할분담만 비교한다.
  private boolean kickoffContentChanged(
      TeamSnapshot beforeTeam,
      Team afterTeam,
      TeamMembersAuditSnapshot beforeMembers,
      TeamMembersAuditSnapshot afterMembers
  ) {
    return !Objects.equals(beforeTeam.kickoffRule(), afterTeam.getKickoffRule())
        || !Objects.equals(beforeTeam.meetingSchedule(), afterTeam.getMeetingSchedule())
        || !beforeMembers.projectRolesEqual(afterMembers);
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

  private record KickoffResult(Team team, List<TeamMember> members) {
  }

  private record TeamNameSnapshot(String name) {
  }

  private record TeamRuleSnapshot(String kickoffRule, String meetingSchedule) {
  }

  private record TeamStatusSnapshot(Status status) {
  }

}
