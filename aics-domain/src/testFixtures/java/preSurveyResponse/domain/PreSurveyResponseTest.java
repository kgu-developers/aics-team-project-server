package preSurveyResponse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;

class PreSurveyResponseTest {

	@Test
	@DisplayName("create는 전달받은 값으로 사전 설문 응답을 만들고 제출일을 찍는다")
	void create() {
		JsonNode roles = JsonConverter.parse("{\"first\":\"BACKEND\",\"second\":\"PM\"}");

		PreSurveyResponse response = PreSurveyResponse.create("202012345", 1L, roles, "웹 서비스", "없음");

		assertThat(response.getUserId()).isEqualTo("202012345");
		assertThat(response.getSectionId()).isEqualTo(1L);
		assertThat(response.getPreferredRoles()).isEqualTo(roles);
		assertThat(response.getTopicOpinion()).isEqualTo("웹 서비스");
		assertThat(response.getEtcOpinion()).isEqualTo("없음");
		assertThat(response.getSubmittedAt()).isNotNull();
		assertThat(response.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("희망 역할이 없으면 저장 전에 막는다 (NOT NULL 컬럼)")
	void createRejectsNullRoles() {
		assertThatThrownBy(() -> PreSurveyResponse.create("202012345", 1L, null, null, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("update는 응답 전체를 갈아끼우고 제출일을 갱신한다")
	void update() {
		PreSurveyResponse response = PreSurveyResponse.create(
				"202012345", 1L, JsonConverter.parse("[\"BACKEND\"]"), "웹 서비스", "없음");

		response.update(JsonConverter.parse("[\"FRONTEND\",\"DESIGN\"]"), null, null);

		assertThat(response.getPreferredRoles().get(0).asText()).isEqualTo("FRONTEND");
		assertThat(response.getTopicOpinion()).isNull();
		assertThat(response.getEtcOpinion()).isNull();
	}

	@Test
	@DisplayName("delete는 삭제일만 찍는다 (소프트 삭제)")
	void delete() {
		PreSurveyResponse response = PreSurveyResponse.create("202012345", 1L, JsonConverter.parse("{}"), null, null);

		response.delete();

		assertThat(response.getDeletedAt()).isNotNull();
	}
}
