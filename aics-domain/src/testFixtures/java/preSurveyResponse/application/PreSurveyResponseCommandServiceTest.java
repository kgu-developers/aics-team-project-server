package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import mock.repository.FakePreSurveyResponseRepository;

class PreSurveyResponseCommandServiceTest {

	private FakePreSurveyResponseRepository repository;
	private PreSurveyResponseCommandService commandService;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		repository = new FakePreSurveyResponseRepository();
		commandService = new PreSurveyResponseCommandService(repository);
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("submit은 사전조사 응답이 없으면 새로 생성한다")
	void submit_CreateNew() throws Exception {
		// given
		String userId = "202012345";
		Long sectionId = 1L;
		JsonNode roles = objectMapper.readTree("[\"BACKEND\", \"PM\"]");

		// when
		PreSurveyResponse response = commandService.submit(userId, sectionId, roles, "웹 개발", "특이사항 없음");

		// then
		assertThat(response.getId()).isNotNull();
		assertThat(response.getUserId()).isEqualTo(userId);
		assertThat(response.getSectionId()).isEqualTo(sectionId);
		assertThat(response.getTopicOpinion()).isEqualTo("웹 개발");
	}

	@Test
	@DisplayName("submit은 이미 존재하면 기존 응답을 갱신한다")
	void submit_UpdateExisting() throws Exception {
		// given
		String userId = "202012345";
		Long sectionId = 1L;
		JsonNode initialRoles = objectMapper.readTree("[\"BACKEND\"]");
		PreSurveyResponse initial = commandService.submit(userId, sectionId, initialRoles, "웹 개발", "없음");

		// when
		JsonNode updatedRoles = objectMapper.readTree("[\"FRONTEND\", \"DESIGN\"]");
		PreSurveyResponse updated = commandService.submit(userId, sectionId, updatedRoles, "앱 개발", "수정됨");

		// then
		assertThat(updated.getId()).isEqualTo(initial.getId());
		assertThat(updated.getTopicOpinion()).isEqualTo("앱 개발");
		assertThat(updated.getEtcOpinion()).isEqualTo("수정됨");
	}

	@Test
	@DisplayName("submit 수행 중 동시 생성으로 DataIntegrityViolationException이 발생하더라도 재조회 후 갱신한다")
	void submit_RaceCondition_HandlesConstraintViolation() throws Exception {
		// given
		String userId = "202012345";
		Long sectionId = 1L;
		JsonNode roles1 = objectMapper.readTree("[\"BACKEND\"]");

		// 먼저 다른 트랜잭션이 저장했다고 가상
		PreSurveyResponse competitor = repository.save(PreSurveyResponse.create(userId, sectionId, roles1, "웹 서비스", "최초"));

		// when: 이미 DB에 존재하지만 check-then-act 시점에 못 본 상황을 흉내내어 새로 제출 시도
		// FakePreSurveyResponseRepository.save 는 id가 null인데 active가 존재하면 DataIntegrityViolationException을 발생시킴
		JsonNode roles2 = objectMapper.readTree("[\"FULLSTACK\"]");
		PreSurveyResponse result = commandService.submit(userId, sectionId, roles2, "모바일 앱", "경쟁에서 재조회 후 갱신");

		// then
		assertThat(result.getId()).isEqualTo(competitor.getId());
		assertThat(result.getTopicOpinion()).isEqualTo("모바일 앱");
		assertThat(result.getEtcOpinion()).isEqualTo("경쟁에서 재조회 후 갱신");
	}
}
