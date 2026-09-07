package preSurveyResponse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.common.json.JsonConverter;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;

class PreSurveyResponseTest {

	@Test
	@DisplayName("create는 전달받은 값으로 사전 설문 응답을 만들고 제출일을 찍는다")
	void create() {
		JsonNode roles = JsonConverter.parse("{\"first\":\"BACKEND\",\"second\":\"PM\"}");

		PreSurveyResponse response = PreSurveyResponse.create("202012345", 1L, roles, "웹 서비스", "없음", null);

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
		assertThatThrownBy(() -> PreSurveyResponse.create("202012345", 1L, null, null, null, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("update는 응답 전체를 갈아끼우고 제출일을 갱신한다")
	void update() {
		PreSurveyResponse response = PreSurveyResponse.create(
				"202012345", 1L, JsonConverter.parse("[\"BACKEND\"]"), "웹 서비스", "없음", null);

		response.update(JsonConverter.parse("[\"FRONTEND\",\"DESIGN\"]"), null, null, null);

		assertThat(response.getPreferredRoles().get(0).asText()).isEqualTo("FRONTEND");
		assertThat(response.getTopicOpinion()).isNull();
		assertThat(response.getEtcOpinion()).isNull();
	}

	@Test
	@DisplayName("delete는 삭제일만 찍는다 (소프트 삭제)")
	void delete() {
		PreSurveyResponse response = PreSurveyResponse.create("202012345", 1L, JsonConverter.parse("{}"), null, null, null);

		response.delete();

		assertThat(response.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("희망 조원을 지목해 제출하면 상대 응답 대기(PENDING) 상태가 된다")
	void createWithPreferredPeer() {
		PreSurveyResponse response = PreSurveyResponse.create(
				"202012345", 1L, JsonConverter.parse("[\"BACKEND\"]"), null, null, "202054321");

		assertThat(response.getPreferredPeerUserId()).isEqualTo("202054321");
		assertThat(response.getPreferredPeerStatus()).isEqualTo(PreferredPeerStatus.PENDING);
	}

	@Test
	@DisplayName("같은 학생을 지목한 채 재제출하면 상대가 내린 수락·거절이 유지된다")
	void updateKeepsDecisionWhenPeerUnchanged() {
		PreSurveyResponse response = PreSurveyResponse.create(
				"202012345", 1L, JsonConverter.parse("[\"BACKEND\"]"), null, null, "202054321");
		response.decidePreferredPeer(true);

		response.update(JsonConverter.parse("[\"FRONTEND\"]"), "주제 바꿈", null, "202054321");

		assertThat(response.getPreferredPeerStatus()).isEqualTo(PreferredPeerStatus.ACCEPTED);
	}

	@Test
	@DisplayName("다른 학생으로 바꿔 지목하면 다시 대기 상태가 되고, null 이면 지목이 취소된다")
	void updateResetsOrClearsPreferredPeer() {
		PreSurveyResponse response = PreSurveyResponse.create(
				"202012345", 1L, JsonConverter.parse("[\"BACKEND\"]"), null, null, "202054321");
		response.decidePreferredPeer(true);

		response.update(JsonConverter.parse("[\"BACKEND\"]"), null, null, "202011111");
		assertThat(response.getPreferredPeerUserId()).isEqualTo("202011111");
		assertThat(response.getPreferredPeerStatus()).isEqualTo(PreferredPeerStatus.PENDING);

		response.update(JsonConverter.parse("[\"BACKEND\"]"), null, null, null);
		assertThat(response.getPreferredPeerUserId()).isNull();
		assertThat(response.getPreferredPeerStatus()).isNull();
	}

	@Test
	@DisplayName("isPreferredPeerPendingFor는 대기 중인 지목 대상 본인에게만 참이다")
	void isPreferredPeerPendingFor() {
		PreSurveyResponse response = PreSurveyResponse.create(
				"202012345", 1L, JsonConverter.parse("[\"BACKEND\"]"), null, null, "202054321");

		assertThat(response.isPreferredPeerPendingFor("202054321")).isTrue();
		assertThat(response.isPreferredPeerPendingFor("202011111")).isFalse();

		response.decidePreferredPeer(false);
		assertThat(response.isPreferredPeerPendingFor("202054321")).isFalse();
	}
}
