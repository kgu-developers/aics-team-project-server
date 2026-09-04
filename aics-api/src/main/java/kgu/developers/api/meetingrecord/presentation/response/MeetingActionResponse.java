package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import lombok.Builder;

@Builder
public record MeetingActionResponse(

    @Schema(description = "액션플랜 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
    Long meetingRecordId,

    @Schema(description = "작업 내용", example = "API 명세서 초안 작성", requiredMode = REQUIRED)
    String content,

    @Schema(description = "상태(DONE:완료, IN_PROGRESS:진행중, EXCLUDED:제외)", example = "IN_PROGRESS", requiredMode = REQUIRED)
    MeetingActionStatus status,

    @Schema(description = "담당자 학번", example = "202412345")
    String assigneeId,

    @Schema(description = "마감일시", example = "2026-08-28 18:00")
    String dueAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MeetingActionResponse from(MeetingAction meetingAction) {
        return MeetingActionResponse.builder()
            .id(meetingAction.getId())
            .meetingRecordId(meetingAction.getMeetingRecordId())
            .content(meetingAction.getContent())
            .status(meetingAction.getStatus())
            .assigneeId(meetingAction.getAssigneeId())
            .dueAt(meetingAction.getDueAt() == null ? null : meetingAction.getDueAt().format(FORMATTER))
            .build();
    }
}
