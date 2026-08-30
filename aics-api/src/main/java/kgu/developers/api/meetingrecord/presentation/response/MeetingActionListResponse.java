package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import lombok.Builder;

@Builder
public record MeetingActionListResponse(

    @Schema(description = "액션플랜 목록", requiredMode = REQUIRED)
    List<MeetingActionResponse> contents
) {

    public static MeetingActionListResponse from(List<MeetingAction> meetingActions) {
        return MeetingActionListResponse.builder()
            .contents(meetingActions.stream().map(MeetingActionResponse::from).toList())
            .build();
    }
}
