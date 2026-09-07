package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseRow;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakePreSurveyResponseRepository;
import mock.repository.FakeUserRepository;

class PreSurveyResponseQueryServiceTest {

	private static final Long SECTION_ID = 1L;

	private FakePreSurveyResponseRepository responseRepository;
	private FakeEnrollmentRepository enrollmentRepository;
	private FakeUserRepository userRepository;
	private PreSurveyResponseQueryService queryService;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		responseRepository = new FakePreSurveyResponseRepository();
		enrollmentRepository = new FakeEnrollmentRepository();
		userRepository = new FakeUserRepository();
		queryService = new PreSurveyResponseQueryService(responseRepository, enrollmentRepository, userRepository);
		objectMapper = new ObjectMapper();
	}

	@Test
	@DisplayName("getSectionResponseRows는 활성 수강생만 학번 오름차순으로 담고 조교·철회 학생의 응답은 뺀다")
	void getSectionResponseRows_OnlyActiveStudents() throws Exception {
		// given
		saveStudent("202000001", Role.STUDENT, Status.ACTIVE);   // 응답함
		saveStudent("202000002", Role.STUDENT, Status.ACTIVE);   // 미응답
		saveStudent("202000003", Role.STUDENT, Status.WITHDRAWN); // 탈퇴했지만 응답 있음
		saveStudent("202000004", Role.ASSISTANT, Status.ACTIVE);  // 조교인데 응답 있음

		saveResponse("202000001");
		saveResponse("202000003");
		saveResponse("202000004");

		// when
		List<PreSurveyResponseRow> rows = queryService.getSectionResponseRows(SECTION_ID);

		// then
		assertThat(rows).extracting(PreSurveyResponseRow::userId)
				.containsExactly("202000001", "202000002");
		assertThat(rows.get(0).response()).isNotNull();
		assertThat(rows.get(0).name()).isEqualTo("학생202000001");
		assertThat(rows.get(1).response()).isNull();
	}

	private void saveStudent(String userId, Role role, Status status) {
		enrollmentRepository.save(Enrollment.create(SECTION_ID, userId, role, status));
		userRepository.save(User.create(userId, userId + "@kgu.ac.kr", "학생" + userId,
				"encoded", UserGlobalRole.USER, "010-0000-0000"));
	}

	private void saveResponse(String userId) throws Exception {
		responseRepository.save(PreSurveyResponse.create(userId, SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타"));
	}
}
