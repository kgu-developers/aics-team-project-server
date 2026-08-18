package kgu.developers.api.teammessage.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import lombok.Builder;

@Builder
public record TeamMessagePersistResponse(

    @Schema(description = "생성된 메시지 id", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "스레드 id", example = "1", requiredMode = REQUIRED)
    Long threadId,

    @Schema(description = "발신자 학번", example = "202412345", requiredMode = REQUIRED)
    String senderId,

    @Schema(description = "메시지 본문", example = "다음 회의 일정 문의드립니다.", requiredMode = REQUIRED)
    String message,

    @Schema(description = "메시지 관련 유형", example = "GENERAL", requiredMode = REQUIRED)
    TeamMessageRelatedType relatedType,

    @Schema(description = "관련 리소스 id (폴리모픽, FK 아님)", example = "1", requiredMode = NOT_REQUIRED)
    Long relatedId,

    @Schema(description = "생성일시", example = "2026-08-03 10:00", requiredMode = REQUIRED)
    String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static TeamMessagePersistResponse of(TeamMessage teamMessage) {
        return TeamMessagePersistResponse.builder()
            .id(teamMessage.getId())
            .threadId(teamMessage.getThreadId())
            .senderId(teamMessage.getSenderId())
            .message(teamMessage.getMessage())
            .relatedType(teamMessage.getRelatedType())
            .relatedId(teamMessage.getRelatedId())
            .createdAt(teamMessage.getCreatedAt() != null ? teamMessage.getCreatedAt().format(FORMATTER) : null)
            .build();
    }
}
