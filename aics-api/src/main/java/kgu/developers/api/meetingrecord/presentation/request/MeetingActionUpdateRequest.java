package kgu.developers.api.meetingrecord.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import lombok.Builder;

@Builder
public record MeetingActionUpdateRequest(

    @Schema(description = "작업 내용", example = "수정된 작업 내용")
    String content,

    @Schema(description = "상태(DONE:완료, IN_PROGRESS:진행중, EXCLUDED:제외)", example = "DONE")
    MeetingActionStatus status,

    @Schema(description = "마감일시", example = "2026-08-29T12:00:00")
    LocalDateTime dueAt,

    @Schema(description = "담당자 학번", example = "202412346")
    String assigneeId
) {
}
