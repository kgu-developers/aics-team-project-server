package kgu.developers.api.auditlog.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.TargetType;
import kgu.developers.domain.user.domain.User;
import lombok.Builder;

@Builder
public record TeamHistoryResponse(
        @Schema(description = "감사 로그 식별자", requiredMode = REQUIRED)
        Long id,
        @Schema(description = "행위자 학번", requiredMode = REQUIRED)
        String actorId,
        @Schema(description = "행위자 이름")
        String actorName,
        @Schema(
                description = "프론트 표시 문구 변환에 사용하는 이벤트 유형 문자열",
                example = "TEAM_UPDATED",
                requiredMode = REQUIRED
        )
        String eventType,
        @Schema(
                description = "대상 유형",
                example = "TEAM",
                allowableValues = {"TEAM"},
                requiredMode = REQUIRED
        )
        TargetType targetType,
        @Schema(description = "대상 식별자", requiredMode = REQUIRED)
        Long targetId,
        @Schema(description = "프론트 표시 문구 변환에 사용하는 이벤트별 부가 정보", requiredMode = REQUIRED)
        JsonNode metadata,
        @Schema(description = "발생 시각", requiredMode = REQUIRED)
        LocalDateTime occurredAt
) {
    public static TeamHistoryResponse from(AuditLog auditLog, Map<String, User> actorsById) {
        User actor = actorsById.get(auditLog.getActorId());
        return TeamHistoryResponse.builder()
                .id(auditLog.getId())
                .actorId(auditLog.getActorId())
                .actorName(actor == null ? null : actor.getName())
                .eventType(auditLog.getEventType())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .metadata(auditLog.getMetadata())
                .occurredAt(auditLog.getCreatedAt())
                .build();
    }
}
