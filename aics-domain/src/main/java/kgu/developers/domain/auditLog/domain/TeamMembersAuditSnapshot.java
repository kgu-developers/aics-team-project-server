package kgu.developers.domain.auditLog.domain;

import java.util.Comparator;
import java.util.List;
import kgu.developers.domain.teamMember.domain.TeamMember;

public record TeamMembersAuditSnapshot(List<TeamMemberAuditSnapshot> members) {

  public TeamMembersAuditSnapshot {
    members = List.copyOf(members);
  }

  public static TeamMembersAuditSnapshot from(List<TeamMember> members) {
    if (members == null) {
      return new TeamMembersAuditSnapshot(List.of());
    }
    return new TeamMembersAuditSnapshot(members.stream()
        .map(TeamMemberAuditSnapshot::from)
        .sorted(Comparator.comparing(TeamMemberAuditSnapshot::studentNumber))
        .toList());
  }

  public record TeamMemberAuditSnapshot(
      String studentNumber,
      boolean leader,
      String projectRole
  ) {
    private static TeamMemberAuditSnapshot from(TeamMember member) {
      return new TeamMemberAuditSnapshot(
          member.getUserId(), member.isLeader(), member.getProjectRole());
    }
  }
}
