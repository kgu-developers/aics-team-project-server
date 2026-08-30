package kgu.developers.api.meetingrecord.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import lombok.Builder;

@Builder
public record MeetingActionCreateRequest(

    @Schema(description = "작업 내용", example = "API 명세서 초안 작성", requiredMode = REQUIRED)
    @NotBlank
    String content,

    @Schema(description = "상태(DONE:완료, IN_PROGRESS:진행중, EXCLUDED:제외)", example = "IN_PROGRESS", requiredMode = REQUIRED)
    @NotNull
    MeetingActionStatus status,

    @Schema(description = "마감일시", example = "2026-08-28T18:00:00")
    LocalDateTime dueAt,

    @Schema(description = "담당자 학번", example = "202412345")
    String assigneeId
) {
}
