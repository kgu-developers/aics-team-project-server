package kgu.developers.admin.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import kgu.developers.common.response.PageableResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record MeetingActionAdminPageResponse(

    @Schema(description = "액션플랜 목록", requiredMode = REQUIRED)
    List<MeetingActionAdminResponse> contents,

    @Schema(description = "페이지 정보", requiredMode = REQUIRED)
    PageableResponse<MeetingActionAdminResponse> pageable
) {
    public static MeetingActionAdminPageResponse from(
        Page<MeetingAction> page,
        Map<Long, MeetingRecord> meetingRecordsById,
        Map<Long, Team> teamsById,
        Map<String, User> usersByStudentNumber
    ) {
        List<MeetingActionAdminResponse> contents = page.getContent().stream()
            .map(meetingAction -> {
                MeetingRecord meetingRecord = meetingRecordsById.get(meetingAction.getMeetingRecordId());
                Team team = teamsById.get(meetingRecord.getTeamId());
                User assignee = meetingAction.getAssigneeId() == null
                    ? null
                    : usersByStudentNumber.get(meetingAction.getAssigneeId());
                return MeetingActionAdminResponse.from(meetingAction, meetingRecord, team, assignee);
            })
            .toList();

        PageableResponse<MeetingActionAdminResponse> pageable = PageableResponse.<MeetingActionAdminResponse>builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .isEnd(page.isLast())
            .build();

        return MeetingActionAdminPageResponse.builder()
            .contents(contents)
            .pageable(pageable)
            .build();
    }
}
