package kgu.developers.admin.teamMember.application;

import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import java.util.List;

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

  @Transactional
  public TeamMemberAdminResponse updateTeamMember(
      Long teamId, String studentNumber, TeamMemberUpdateRequest request) {
    TeamMember teamMember = teamMemberQueryService.getTeamMember(teamId, studentNumber);

    TeamMember updated = teamMemberCommandService.updateTeamMember(
        teamMember, request.targetTeamId(), request.projectRole(), request.isLeader());
    User user = userQueryService.getUsersByStudentNumbers(List.of(studentNumber)).stream()
        .findFirst()
        .orElse(null);
    return TeamMemberAdminResponse.of(updated, user);
  }
}
