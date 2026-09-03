package kgu.developers.api.auditlog.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import kgu.developers.api.auditlog.presentation.response.TeamActivitySummaryResponse;
import kgu.developers.api.auditlog.presentation.response.TeamHistoryPageResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.auditLog.application.query.AuditLogQueryService;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogFacade {

    private final TeamAccessValidator teamAccessValidator;
    private final TeamMemberQueryService teamMemberQueryService;
    private final AuditLogQueryService auditLogQueryService;
    private final UserQueryService userQueryService;

    public TeamHistoryPageResponse getTeamHistories(Long teamId, Pageable pageable, String userId) {
        Team team = teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        Page<AuditLog> histories = auditLogQueryService.getTeamHistories(team.getSectionId(), teamId, pageable);
        List<User> actors = userQueryService.getUsersByStudentNumbers(actorIds(histories.getContent()));
        return TeamHistoryPageResponse.from(histories, actors);
    }

    public TeamActivitySummaryResponse getTeamActivitySummary(Long teamId, String userId) {
        Team team = teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        List<TeamMember> members = teamMemberQueryService.getTeamMembersByTeamId(teamId);
        List<String> memberIds = members.stream()
                .map(TeamMember::getUserId)
                .distinct()
                .toList();
        List<User> users = userQueryService.getUsersByStudentNumbers(memberIds);
        List<AuditLog> activities = auditLogQueryService.getMemberActivities(team.getSectionId(), teamId, memberIds);
        return TeamActivitySummaryResponse.from(teamId, members, users, activities);
    }

    private List<String> actorIds(List<AuditLog> histories) {
        return histories.stream()
                .map(AuditLog::getActorId)
                .distinct()
                .toList();
    }
}
