package preSurveyResponse.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredRolesInvalidException;
import kgu.developers.domain.preSurveyResponse.infrastructure.PreSurveyResponseJpaEntity;

class PreSurveyResponseJpaEntityTest {

	@Test
	@DisplayName("어떤 형식의 희망 역할 JSON이든 저장되고 그대로 복원된다 (한글·숫자·boolean 포함)")
	void jsonRoundTrip() {
		JsonNode roles = JsonConverter.parse("""
				{"ranked":[{"role":"백엔드","priority":1,"experienced":true},\
				{"role":"PM","priority":2,"experienced":false}]}""");
		PreSurveyResponse origin = PreSurveyResponse.create("202012345", 1L, roles, "웹 서비스", null);

		PreSurveyResponseJpaEntity entity = PreSurveyResponseJpaEntity.toEntity(origin);
		assertThat(entity.getPreferredRoles()).contains("\"role\":\"백엔드\"", "\"priority\":1");

		PreSurveyResponse restored = entity.toDomain();
		assertThat(restored.getPreferredRoles()).isEqualTo(roles);
		assertThat(restored.getPreferredRoles().at("/ranked/0/role").asText()).isEqualTo("백엔드");
		assertThat(restored.getPreferredRoles().at("/ranked/1/experienced").asBoolean()).isFalse();
		assertThat(restored.getTopicOpinion()).isEqualTo("웹 서비스");
		assertThat(restored.getEtcOpinion()).isNull();
		assertThat(restored.getSubmittedAt()).isEqualTo(origin.getSubmittedAt());
	}

	@Test
	@DisplayName("컬럼 값이 JSON이 아니면 내용을 노출하지 않고 던진다")
	void brokenJsonDoesNotLeakContent() {
		PreSurveyResponseJpaEntity broken = PreSurveyResponseJpaEntity.builder()
				.id(7L)
				.userId("202012345")
				.sectionId(1L)
				.preferredRoles("{비밀 메모")
				.submittedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
				.build();

		assertThatThrownBy(broken::toDomain)
				.isInstanceOf(PreSurveyResponsePreferredRolesInvalidException.class)
				.hasMessageNotContaining("비밀 메모");
	}

	@Test
	@DisplayName("toEntity는 기존 응답의 생성일과 삭제일을 그대로 옮긴다")
	void toEntityKeepsTimestamps() {
		LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
		LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
		PreSurveyResponse stored = PreSurveyResponse.builder()
				.id(1L)
				.userId("202012345")
				.sectionId(1L)
				.preferredRoles(JsonConverter.parse("{}"))
				.submittedAt(createdAt)
				.createdAt(createdAt)
				.deletedAt(deletedAt)
				.build();

		PreSurveyResponseJpaEntity entity = PreSurveyResponseJpaEntity.toEntity(stored);

		assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
		assertThat(entity.toDomain().getId()).isEqualTo(1L);
	}
}
