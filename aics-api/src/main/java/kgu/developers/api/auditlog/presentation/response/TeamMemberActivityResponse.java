package kgu.developers.api.auditlog.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record TeamMemberActivityResponse(
        @Schema(description = "팀원 학번", requiredMode = REQUIRED)
        String userId,
        @Schema(description = "팀원 이름")
        String name,
        @Schema(description = "팀장 여부", requiredMode = REQUIRED)
        boolean leader,
        @Schema(description = "프로젝트 역할")
        String projectRole,
        @Schema(description = "마지막 로그인 시각")
        LocalDateTime lastLoginAt,
        @Schema(description = "마지막 활동")
        LatestActivityResponse lastActivity
) {
    public static TeamMemberActivityResponse from(TeamMember member, User user, AuditLog latestActivity) {
        return TeamMemberActivityResponse.builder()
                .userId(member.getUserId())
                .name(user == null ? null : user.getName())
                .leader(member.isLeader())
                .projectRole(member.getProjectRole())
                .lastLoginAt(user == null ? null : user.getLastLoginAt())
                .lastActivity(LatestActivityResponse.from(latestActivity))
                .build();
    }
}
