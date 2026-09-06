package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record MeetingActionResponse(

    @Schema(description = "액션플랜 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
    Long meetingRecordId,

    @Schema(description = "작업 내용", example = "API 명세서 초안 작성", requiredMode = REQUIRED)
    String content,

    @Schema(description = "상태(TODO:시작 전, IN_PROGRESS:진행중, DONE:완료)", example = "IN_PROGRESS", requiredMode = REQUIRED)
    MeetingActionStatus status,

    @Schema(description = "담당자(팀원 전체 중 지정)")
    AssigneeResponse assignee,

    @Schema(description = "마감일시", example = "2026-08-28 18:00")
    String dueAt,

    @Schema(description = "생성일", example = "2026-08-01 10:00", requiredMode = REQUIRED)
    String createdAt,

    @Schema(description = "수정일", example = "2026-08-02 09:30", requiredMode = REQUIRED)
    String updatedAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MeetingActionResponse from(MeetingAction meetingAction, User assignee) {
        return MeetingActionResponse.builder()
            .id(meetingAction.getId())
            .meetingRecordId(meetingAction.getMeetingRecordId())
            .content(meetingAction.getContent())
            .status(meetingAction.getStatus())
            .assignee(AssigneeResponse.from(assignee))
            .dueAt(meetingAction.getDueAt() == null ? null : meetingAction.getDueAt().format(FORMATTER))
            .createdAt(meetingAction.getCreatedAt().format(FORMATTER))
            .updatedAt(meetingAction.getUpdatedAt().format(FORMATTER))
            .build();
    }

    public record AssigneeResponse(
        @Schema(description = "담당자 학번", example = "202412345", requiredMode = REQUIRED)
        String userId,

        @Schema(description = "담당자 이름", example = "홍길동", requiredMode = REQUIRED)
        String name
    ) {
        public static AssigneeResponse from(User user) {
            return user == null ? null : new AssigneeResponse(user.getStudentNumber(), user.getName());
        }
    }
}
