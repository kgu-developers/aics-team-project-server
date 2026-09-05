package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record TeamMeetingActionListResponse(

    @Schema(description = "팀 전체 액션플랜 목록", requiredMode = REQUIRED)
    List<TeamMeetingActionResponse> contents
) {
}
