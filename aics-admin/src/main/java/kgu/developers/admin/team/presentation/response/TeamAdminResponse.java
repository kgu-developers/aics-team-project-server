package kgu.developers.admin.team.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;

public record TeamAdminResponse(
    @Schema(description = "팀 ID", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "팀명", example = "1팀", requiredMode = REQUIRED)
    String name,

    @Schema(description = "팀 운영규칙", example = "매주 화요일 회고")
    String kickoffRule,

    @Schema(description = "정기 회의일정", example = "매주 목 19:00")
    String meetingSchedule,

    @Schema(description = "상태", example = "FORMING", requiredMode = REQUIRED)
    Status status,

    @Schema(description = "생성 일시")
    LocalDateTime createdAt
) {

    public static TeamAdminResponse from(Team team) {
        return new TeamAdminResponse(
            team.getId(),
            team.getName(),
            team.getKickoffRule(),
            team.getMeetingSchedule(),
            team.getStatus(),
            team.getCreatedAt()
        );
    }
}
