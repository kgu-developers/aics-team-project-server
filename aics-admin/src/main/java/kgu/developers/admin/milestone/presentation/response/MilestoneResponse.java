package kgu.developers.admin.milestone.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;

public record MilestoneResponse(
        @Schema(description = "마일스톤 ID", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "분반 ID", example = "1", requiredMode = REQUIRED)
        Long sectionId,

        @Schema(description = "마일스톤 제목", example = "프로젝트 제안서", requiredMode = REQUIRED)
        String title,

        @Schema(description = "마일스톤 설명")
        String description,

        @Schema(description = "진행 주차", example = "2", requiredMode = REQUIRED)
        int weekNumber,

        @Schema(description = "공개 상태", example = "DRAFT", requiredMode = REQUIRED)
        MilestoneStatus status,

        @Schema(description = "마일스톤 일정", requiredMode = REQUIRED)
        MilestoneScheduleResponse schedule,

        @Schema(description = "마일스톤 유형", example = "GENERAL", requiredMode = REQUIRED)
        MilestoneType type
) {
    public static MilestoneResponse from(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getSectionId(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getWeekNumber(),
                milestone.getStatus(),
                MilestoneScheduleResponse.from(milestone.getSchedule()),
                milestone.getType()
        );
    }
}
