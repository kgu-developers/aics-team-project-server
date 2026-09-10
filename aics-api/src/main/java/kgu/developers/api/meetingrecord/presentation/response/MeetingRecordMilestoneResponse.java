package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneType;
import lombok.Builder;

@Builder
public record MeetingRecordMilestoneResponse(

    @Schema(description = "마일스톤 식별자", example = "3", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "마일스톤 제목", example = "중간 보고서", requiredMode = REQUIRED)
    String title,

    @Schema(description = "마일스톤 유형", example = "MID_REPORT", requiredMode = REQUIRED)
    MilestoneType type,

    @Schema(description = "주차", example = "8", requiredMode = REQUIRED)
    int weekNumber
) {
    public static MeetingRecordMilestoneResponse from(Milestone milestone) {
        return MeetingRecordMilestoneResponse.builder()
            .id(milestone.getId())
            .title(milestone.getTitle())
            .type(milestone.getType())
            .weekNumber(milestone.getWeekNumber())
            .build();
    }
}
