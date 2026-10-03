package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record MeetingRecordEditLogResponse(

    @Schema(description = "수정 이력 식별자", example = "3", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "회의록 식별자", example = "7", requiredMode = REQUIRED)
    Long meetingRecordId,

    @Schema(description = "수정자 학번", example = "202412345", requiredMode = REQUIRED)
    String editorId,

    @Schema(description = "수정자 이름", example = "홍길동")
    String editorName,

    @Schema(description = "수정 사유", example = "다음 주 발표 준비 담당자가 잘못 기재되어 실제 논의대로 수정했습니다.", requiredMode = REQUIRED)
    String reason,

    @Schema(description = "수정 일시", example = "2026-10-04 15:30", requiredMode = REQUIRED)
    String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MeetingRecordEditLogResponse from(MeetingRecordEditLog editLog, User editor) {
        return MeetingRecordEditLogResponse.builder()
            .id(editLog.getId())
            .meetingRecordId(editLog.getMeetingRecordId())
            .editorId(editLog.getEditorId())
            .editorName(editor == null ? null : editor.getName())
            .reason(editLog.getReason())
            .createdAt(editLog.getCreatedAt() == null ? null : editLog.getCreatedAt().format(FORMATTER))
            .build();
    }
}
