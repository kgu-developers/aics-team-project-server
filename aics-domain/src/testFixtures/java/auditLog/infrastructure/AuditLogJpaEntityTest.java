package auditLog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.TargetType;
import kgu.developers.domain.auditLog.exception.AuditLogMetadataInvalidException;
import kgu.developers.domain.auditLog.infrastructure.AuditLogJpaEntity;

class AuditLogJpaEntityTest {

	@Test
	@DisplayName("AuditLog와 AuditLogJpaEntity 상호 변환 시 모든 필드가 올바르게 유지된다")
	void conversionRoundTrip() {
		JsonNode metadata = JsonConverter.parse("{\"reason\":\"policy violation\",\"severity\":\"HIGH\"}");
		AuditLog origin = AuditLog.create("202012345", 1L, "USER_BAN", TargetType.USER, 55L, metadata);

		AuditLogJpaEntity entity = AuditLogJpaEntity.toEntity(origin);

		assertThat(entity.getActorId()).isEqualTo("202012345");
		assertThat(entity.getSectionId()).isEqualTo(1L);
		assertThat(entity.getEventType()).isEqualTo("USER_BAN");
		assertThat(entity.getTargetType()).isEqualTo(TargetType.USER.getCode());
		assertThat(entity.getTargetId()).isEqualTo(55L);
		assertThat(entity.getMetadata()).contains("\"severity\":\"HIGH\"");

		AuditLog restored = entity.toDomain();
		assertThat(restored.getActorId()).isEqualTo(origin.getActorId());
		assertThat(restored.getSectionId()).isEqualTo(origin.getSectionId());
		assertThat(restored.getEventType()).isEqualTo(origin.getEventType());
		assertThat(restored.getTargetType()).isEqualTo(origin.getTargetType());
		assertThat(restored.getTargetId()).isEqualTo(origin.getTargetId());
		assertThat(restored.getMetadata()).isEqualTo(metadata);
	}

	@Test
	@DisplayName("잘못된 형식의 JSON 메타데이터인 경우 AuditLogMetadataInvalidException이 발생한다")
	void invalidMetadataThrowsException() {
		AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
				.id(1L)
				.actorId("202012345")
				.sectionId(1L)
				.eventType("USER_BAN")
				.targetType(TargetType.USER.getCode())
				.targetId(55L)
				.metadata("{invalid_json_format")
				.build();

		assertThatThrownBy(entity::toDomain)
				.isInstanceOf(AuditLogMetadataInvalidException.class);
	}

	@Test
	@DisplayName("toEntity는 도메인 생성일시 및 삭제일시를 보존한다")
	void toEntityPreservesTimestamps() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 8, 20, 10, 0);
		LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 21, 10, 0);

		AuditLog origin = AuditLog.builder()
				.id(10L)
				.actorId("202012345")
				.sectionId(1L)
				.eventType("EVENT")
				.targetType(TargetType.USER)
				.targetId(1L)
				.metadata(JsonConverter.parse("{}"))
				.createdAt(createdAt)
				.deletedAt(deletedAt)
				.build();

		AuditLogJpaEntity entity = AuditLogJpaEntity.toEntity(origin);

		assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
	}
}
