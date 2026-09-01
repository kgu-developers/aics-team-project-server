package kgu.developers.api.auditlog.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record TeamActivitySummaryResponse(
        @Schema(description = "팀 식별자", requiredMode = REQUIRED)
        Long teamId,
        @Schema(description = "팀원 활동 요약", requiredMode = REQUIRED)
        List<TeamMemberActivityResponse> members
) {
    public static TeamActivitySummaryResponse from(
            Long teamId,
            List<TeamMember> members,
            List<User> users,
            List<AuditLog> activities
    ) {
        Map<String, User> usersById = users.stream()
                .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
        Set<String> memberIds = members.stream()
                .map(TeamMember::getUserId)
                .collect(Collectors.toSet());
        Map<String, AuditLog> latestActivityByUserId = activities.stream()
                .filter(activity -> memberIds.contains(activity.getActorId()))
                .collect(Collectors.toMap(
                        AuditLog::getActorId,
                        Function.identity(),
                        BinaryOperator.maxBy(activityOrder())
                ));
        List<TeamMemberActivityResponse> summaries = members.stream()
                .sorted(Comparator.comparing(TeamMember::getUserId))
                .map(member -> TeamMemberActivityResponse.from(
                        member,
                        usersById.get(member.getUserId()),
                        latestActivityByUserId.get(member.getUserId())
                ))
                .toList();
        return new TeamActivitySummaryResponse(teamId, summaries);
    }

    private static Comparator<AuditLog> activityOrder() {
        return Comparator
                .comparing(AuditLog::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(AuditLog::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
    }
}
