package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record TeamMeetingActionResponse(

    @Schema(description = "액션플랜 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
    Long meetingRecordId,

    @Schema(description = "작업 내용", example = "API 명세서 초안 작성", requiredMode = REQUIRED)
    String content,

    @Schema(description = "상태(TODO:시작 전, IN_PROGRESS:진행중, DONE:완료)", example = "IN_PROGRESS", requiredMode = REQUIRED)
    MeetingActionStatus status,

    @Schema(description = "담당자(팀원 전체 중 지정)")
    MeetingActionResponse.AssigneeResponse assignee,

    @Schema(description = "마감일시", example = "2026-08-28 18:00")
    String dueAt,

    @Schema(description = "생성일", example = "2026-08-01 10:00", requiredMode = REQUIRED)
    String createdAt,

    @Schema(description = "수정일", example = "2026-08-02 09:30", requiredMode = REQUIRED)
    String updatedAt,

    @Schema(description = "소속 회의록", requiredMode = REQUIRED)
    MeetingRecordSummary meetingRecord
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static TeamMeetingActionResponse from(MeetingAction meetingAction, User assignee, MeetingRecord meetingRecord) {
        return TeamMeetingActionResponse.builder()
            .id(meetingAction.getId())
            .meetingRecordId(meetingAction.getMeetingRecordId())
            .content(meetingAction.getContent())
            .status(meetingAction.getStatus())
            .assignee(MeetingActionResponse.AssigneeResponse.from(assignee))
            .dueAt(meetingAction.getDueAt() == null ? null : meetingAction.getDueAt().format(FORMATTER))
            .createdAt(meetingAction.getCreatedAt().format(FORMATTER))
            .updatedAt(meetingAction.getUpdatedAt().format(FORMATTER))
            .meetingRecord(new MeetingRecordSummary(meetingRecord.getId(), meetingRecord.getTitle()))
            .build();
    }

    public record MeetingRecordSummary(

        @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "회의록 제목", example = "3주차 정기 회의", requiredMode = REQUIRED)
        String title
    ) {
    }
}
