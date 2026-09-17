package preSurveyResponse.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import kgu.developers.domain.user.domain.UserRepository;
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

	@Test
	@DisplayName("희망 조원이 미응답이어도 엑셀 행에 이름을 담는다")
	void getSectionResponseRows_FillsPreferredPeerName() throws Exception {
		saveStudent("202000001", Role.STUDENT, Status.ACTIVE);
		saveStudent("202000002", Role.STUDENT, Status.ACTIVE);
		responseRepository.save(PreSurveyResponse.create("202000001", SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타", "202000002"));

		List<PreSurveyResponseRow> rows = queryService.getSectionResponseRows(SECTION_ID);

		assertThat(rows.get(0).preferredPeerName()).isEqualTo("학생202000002");
	}

	@Test
	@DisplayName("서로 지목한 학생만 엑셀 행에 매칭으로 표시한다")
	void getSectionResponseRows_MarksMutualPeers() throws Exception {
		saveStudent("202000001", Role.STUDENT, Status.ACTIVE);
		saveStudent("202000002", Role.STUDENT, Status.ACTIVE);
		saveStudent("202000003", Role.STUDENT, Status.ACTIVE);
		responseRepository.save(PreSurveyResponse.create("202000001", SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타", "202000002"));
		responseRepository.save(PreSurveyResponse.create("202000002", SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타", "202000001"));
		responseRepository.save(PreSurveyResponse.create("202000003", SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타", "202000001"));

		List<PreSurveyResponseRow> rows = queryService.getSectionResponseRows(SECTION_ID);

		assertThat(rows.get(0).mutual()).isTrue();
		assertThat(rows.get(1).mutual()).isTrue();
		// 한쪽만 지목한 응답은 상대가 수락하기 전까지 매칭이 아니다
		assertThat(rows.get(2).mutual()).isFalse();
	}

	@Test
	@DisplayName("출력하지 않는 조교와 철회 학생의 희망 조원은 이름 조회 대상에서 뺀다")
	void getSectionResponseRows_LooksUpPreferredPeersOfActiveStudentsOnly() throws Exception {
		FakePreSurveyResponseRepository responses = new FakePreSurveyResponseRepository();
		FakeEnrollmentRepository enrollments = new FakeEnrollmentRepository();
		UserRepository users = mock(UserRepository.class);
		enrollments.save(Enrollment.create(SECTION_ID, "202000001", Role.STUDENT, Status.ACTIVE));
		enrollments.save(Enrollment.create(SECTION_ID, "202000002", Role.ASSISTANT, Status.ACTIVE));
		responses.save(PreSurveyResponse.create("202000001", SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타", "202000003"));
		responses.save(PreSurveyResponse.create("202000002", SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타", "202000004"));
		given(users.findAllByStudentNumberIn(List.of("202000001", "202000003"))).willReturn(List.of(
				User.create("202000001", "a@kgu.ac.kr", "학생A", "encoded", UserGlobalRole.USER, null),
				User.create("202000003", "c@kgu.ac.kr", "학생C", "encoded", UserGlobalRole.USER, null)));

		new PreSurveyResponseQueryService(responses, enrollments, users).getSectionResponseRows(SECTION_ID);

		verify(users).findAllByStudentNumberIn(List.of("202000001", "202000003"));
	}

	private void saveStudent(String userId, Role role, Status status) {
		enrollmentRepository.save(Enrollment.create(SECTION_ID, userId, role, status));
		userRepository.save(User.create(userId, userId + "@kgu.ac.kr", "학생" + userId,
				"encoded", UserGlobalRole.USER, "010-0000-0000"));
	}

	private void saveResponse(String userId) throws Exception {
		responseRepository.save(PreSurveyResponse.create(userId, SECTION_ID,
				objectMapper.readTree("[\"BACKEND\"]"), "주제", "기타", null));
	}
}
