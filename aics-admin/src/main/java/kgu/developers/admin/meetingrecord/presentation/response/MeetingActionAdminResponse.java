package kgu.developers.admin.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record MeetingActionAdminResponse(

    @Schema(description = "액션플랜 식별자", example = "12", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "팀 식별자", example = "20", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "1팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "회의록 식별자", example = "7", requiredMode = REQUIRED)
    Long meetingRecordId,

    @Schema(description = "회의록 제목", example = "3주차 정기 회의", requiredMode = REQUIRED)
    String meetingRecordTitle,

    @Schema(description = "회의 일시", example = "2026-09-30 14:00", requiredMode = REQUIRED)
    String meetingAt,

    @Schema(description = "담당자 학번(미지정이면 null)", example = "202412345")
    String assigneeId,

    @Schema(description = "담당자 이름(미지정이면 null)", example = "홍길동")
    String assigneeName,

    @Schema(description = "작업 내용", example = "로그인 API 연동", requiredMode = REQUIRED)
    String content,

    @Schema(description = "상태(TODO:시작 전, IN_PROGRESS:진행중, DONE:완료)", example = "IN_PROGRESS", requiredMode = REQUIRED)
    MeetingActionStatus status,

    @Schema(description = "마감일시(미지정이면 null)", example = "2026-10-07 23:59")
    String dueAt,

    @Schema(description = "생성일", example = "2026-09-30 15:00", requiredMode = REQUIRED)
    String createdAt,

    @Schema(description = "수정일", example = "2026-10-01 09:30", requiredMode = REQUIRED)
    String updatedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MeetingActionAdminResponse from(
        MeetingAction meetingAction,
        MeetingRecord meetingRecord,
        Team team,
        User assignee
    ) {
        return MeetingActionAdminResponse.builder()
            .id(meetingAction.getId())
            .teamId(team.getId())
            .teamName(team.getName())
            .meetingRecordId(meetingRecord.getId())
            .meetingRecordTitle(meetingRecord.getTitle())
            .meetingAt(format(meetingRecord.getMeetingAt()))
            .assigneeId(meetingAction.getAssigneeId())
            .assigneeName(assignee == null ? null : assignee.getName())
            .content(meetingAction.getContent())
            .status(meetingAction.getStatus())
            .dueAt(format(meetingAction.getDueAt()))
            .createdAt(format(meetingAction.getCreatedAt()))
            .updatedAt(format(meetingAction.getUpdatedAt()))
            .build();
    }

    private static String format(LocalDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }
}
