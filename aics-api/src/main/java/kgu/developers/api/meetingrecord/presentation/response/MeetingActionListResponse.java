package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record MeetingActionListResponse(

    @Schema(description = "액션플랜 목록", requiredMode = REQUIRED)
    List<MeetingActionResponse> contents
) {

    public static MeetingActionListResponse from(List<MeetingAction> meetingActions, Map<String, User> usersByStudentNumber) {
        return MeetingActionListResponse.builder()
            .contents(meetingActions.stream()
                .map(action -> MeetingActionResponse.from(action, usersByStudentNumber.get(action.getAssigneeId())))
                .toList())
            .build();
    }
}
