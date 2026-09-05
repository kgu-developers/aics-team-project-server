package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakePreSurveyResponseRepository;

class PreSurveyResponseCommandServiceTest {

	private static final String USER_ID = "202012345";
	private static final Long SECTION_ID = 1L;

	private FakePreSurveyResponseRepository repository;
	private FakeEnrollmentRepository enrollmentRepository;
	private PreSurveyResponseCommandService commandService;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		repository = new FakePreSurveyResponseRepository();
		enrollmentRepository = new FakeEnrollmentRepository();
		enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
		commandService = new PreSurveyResponseCommandService(repository, enrollmentRepository);
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("submit은 사전조사 응답이 없으면 새로 생성한다")
	void submit_CreateNew() throws Exception {
		// given
		String userId = USER_ID;
		Long sectionId = SECTION_ID;
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
	@DisplayName("submit은 그 분반에 Enrollment가 없는 사용자는 거부한다")
	void submit_RejectsWhenEnrollmentMissing() throws Exception {
		// given
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");

		// when & then
		assertThatThrownBy(() -> commandService.submit(
				"202099999", SECTION_ID, roles, "웹 개발", "없음"))
				.isInstanceOf(kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException.class);
	}
}
