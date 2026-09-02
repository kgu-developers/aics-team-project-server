package kgu.developers.api.auditlog.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.TargetType;
import lombok.Builder;

@Builder
public record LatestActivityResponse(
        @Schema(
                description = "프론트 표시 문구 변환에 사용하는 이벤트 유형 문자열",
                example = "TEAM_UPDATED",
                requiredMode = REQUIRED
        )
        String eventType,
        @Schema(
                description = "대상 유형",
                example = "TEAM",
                allowableValues = {"TEAM", "USER", "MEETING_RECORD", "SUBMISSION", "MILESTONE"},
                requiredMode = REQUIRED
        )
        TargetType targetType,
        @Schema(description = "대상 식별자", requiredMode = REQUIRED)
        Long targetId,
        @Schema(description = "마지막 활동 시각", requiredMode = REQUIRED)
        LocalDateTime occurredAt
) {
    public static LatestActivityResponse from(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return LatestActivityResponse.builder()
                .eventType(auditLog.getEventType())
                .targetType(auditLog.getTargetType())
                .targetId(auditLog.getTargetId())
                .occurredAt(auditLog.getCreatedAt())
                .build();
    }
}
