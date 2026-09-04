package auditLog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.auditLog.domain.AuditLog;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
import kgu.developers.domain.auditLog.domain.TargetType;

class AuditLogTest {

	@Test
	@DisplayName("create는 전달받은 인자로 AuditLog를 생성한다")
	void create() {
		JsonNode metadata = JsonConverter.parse("{\"action\":\"CREATE_TEAM\",\"details\":\"Team 1 created\"}");

		AuditLog auditLog = AuditLog.create("202012345", 1L, AuditLogEventType.TEAM_UPDATED, TargetType.TEAM, 10L, metadata);

		assertThat(auditLog.getActorId()).isEqualTo("202012345");
		assertThat(auditLog.getSectionId()).isEqualTo(1L);
		assertThat(auditLog.getEventType()).isEqualTo(AuditLogEventType.TEAM_UPDATED);
		assertThat(auditLog.getTargetType()).isEqualTo(TargetType.TEAM);
		assertThat(auditLog.getTargetId()).isEqualTo(10L);
		assertThat(auditLog.getMetadata()).isEqualTo(metadata);
		assertThat(auditLog.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("필수 인자가 null이면 NullPointerException이 발생한다")
	void createRejectsNullFields() {
		JsonNode metadata = JsonConverter.parse("{}");

		assertThatThrownBy(() -> AuditLog.create(null, 1L, AuditLogEventType.TEAM_UPDATED, TargetType.USER, 1L, metadata))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> AuditLog.create("202012345", null, AuditLogEventType.TEAM_UPDATED, TargetType.USER, 1L, metadata))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> AuditLog.create("202012345", 1L, null, TargetType.USER, 1L, metadata))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> AuditLog.create("202012345", 1L, AuditLogEventType.TEAM_UPDATED, null, 1L, metadata))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> AuditLog.create("202012345", 1L, AuditLogEventType.TEAM_UPDATED, TargetType.USER, null, metadata))
				.isInstanceOf(NullPointerException.class);

		assertThatThrownBy(() -> AuditLog.create("202012345", 1L, AuditLogEventType.TEAM_UPDATED, TargetType.USER, 1L, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("delete는 소프트 삭제 일시를 설정한다")
	void delete() {
		JsonNode metadata = JsonConverter.parse("{}");
		AuditLog auditLog = AuditLog.create("202012345", 1L, AuditLogEventType.TEAM_UPDATED, TargetType.USER, 1L, metadata);

		auditLog.delete();
		assertThat(auditLog.getDeletedAt()).isNotNull();

		LocalDateTime fixedNow = LocalDateTime.of(2026, 8, 21, 12, 0);
		auditLog.delete(fixedNow);
		assertThat(auditLog.getDeletedAt()).isEqualTo(fixedNow);
	}
}
