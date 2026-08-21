package kgu.developers.domain.auditLog.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class AuditLog {
    private Long id;

    private String actorId;  // 행위자 학번
    private Long sectionId;  // 분반 식별자

    private String eventType;  // 이벤트 유형
    private Long targetType;  // 대상 유형
    private Long targetId;  // 대상 식별자
    private JsonNode metadata;  // 부가정보

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static AuditLog create(String actorId, Long sectionId, String eventType, Long targetType, Long targetId, JsonNode metadata) {
        return AuditLog.builder()
                .actorId(requireNonNull(actorId, "actorId"))
                .sectionId(requireNonNull(sectionId, "sectionId"))
                .eventType(requireNonNull(eventType, "eventType"))
                .targetType(requireNonNull(targetType, "targetType"))
                .targetId(requireNonNull(targetId, "targetId"))
                .metadata(requireNonNull(metadata, "metadata"))
                .build();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
