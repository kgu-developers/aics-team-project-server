package kgu.developers.domain.auditLog.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

  /**
   * 제안서에 표시되는 역할분담(projectRole)만 비교한다.
   * 팀장 여부는 제안서 항목이 아니므로 제외한다.
   */
  public boolean projectRolesEqual(TeamMembersAuditSnapshot other) {
    if (members.size() != other.members.size()) {
      return false;
    }
    for (int i = 0; i < members.size(); i++) {
      if (!members.get(i).projectRoleEqual(other.members.get(i))) {
        return false;
      }
    }
    return true;
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

    /**
     * 제안서에 표시되는 역할분담만 비교한다.
     */
    private boolean projectRoleEqual(TeamMemberAuditSnapshot other) {
      return Objects.equals(studentNumber, other.studentNumber)
          && Objects.equals(projectRole, other.projectRole);
    }
  }
}
