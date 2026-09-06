package kgu.developers.api.meetingrecord.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record MeetingActionCreateRequest(

    @Schema(description = "작업 내용", example = "API 명세서 초안 작성", requiredMode = REQUIRED)
    @NotBlank
    String content,

    @Schema(description = "담당자 학번(팀원 전체 중 지정, 참석자로 한정하지 않음)", example = "202412345")
    String assigneeId,

    @Schema(description = "마감일시", example = "2026-08-28T18:00:00")
    LocalDateTime dueAt
) {
}
