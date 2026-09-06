package presurvey.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.api.preSurveyResponse.application.PreSurveyResponseFacade;
import kgu.developers.api.preSurveyResponse.presentation.request.PreSurveyResponseSubmitRequest;
import kgu.developers.api.preSurveyResponse.presentation.response.PreSurveyResponseDetailResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseNotFoundException;
import mock.repository.FakePreSurveyResponseRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreSurveyResponseFacadeTest {

	private static final Long SECTION_ID = 1L;
	private static final String STUDENT = "202012345";
	private static final String OUTSIDER = "202000000";
	private static final String WITHDRAWN_STUDENT = "202099999";
	private static final String ASSISTANT = "202088888";

	@Mock
	private EnrollmentRepository enrollmentRepository;

	private PreSurveyResponseFacade preSurveyResponseFacade;

	@BeforeEach
	void init() {
		FakePreSurveyResponseRepository repository = new FakePreSurveyResponseRepository();
		preSurveyResponseFacade = new PreSurveyResponseFacade(
				new PreSurveyResponseCommandService(repository, enrollmentRepository),
				new PreSurveyResponseQueryService(repository),
				enrollmentRepository
		);
		given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, STUDENT))
				.willReturn(Optional.of(Enrollment.create(SECTION_ID, STUDENT, Role.STUDENT, Status.ACTIVE)));
		given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, OUTSIDER))
				.willReturn(Optional.empty());
		given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, WITHDRAWN_STUDENT))
				.willReturn(Optional.of(Enrollment.create(SECTION_ID, WITHDRAWN_STUDENT, Role.STUDENT, Status.WITHDRAWN)));
		given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, ASSISTANT))
				.willReturn(Optional.of(Enrollment.create(SECTION_ID, ASSISTANT, Role.ASSISTANT, Status.ACTIVE)));
		// PreSurveyResponseCommandService.submit()이 동시제출 방지용으로 같은 Enrollment 행을
		// findBySectionIdAndUserIdForUpdate로 다시 잠그므로(sunzx0428 PR #65 리뷰 09-03 대응),
		// 위와 동일한 스텁을 이 메서드에도 걸어준다 — 정상 케이스(STUDENT)만 필요.
		given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, STUDENT))
				.willReturn(Optional.of(Enrollment.create(SECTION_ID, STUDENT, Role.STUDENT, Status.ACTIVE)));
	}

	private PreSurveyResponseSubmitRequest request(List<String> roles, String topicOpinion) {
		return PreSurveyResponseSubmitRequest.builder()
				.preferredRoles(roles)
				.topicOpinion(topicOpinion)
				.etcOpinion("없음")
				.build();
	}

	@Test
	@DisplayName("submit은 희망 역할을 JSON 배열로 저장한 응답을 돌려준다")
	void submit() {
		PreSurveyResponseDetailResponse result =
				preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND", "PM"), "웹 서비스"));

		assertThat(result.id()).isNotNull();
		assertThat(result.userId()).isEqualTo(STUDENT);
		assertThat(result.sectionId()).isEqualTo(SECTION_ID);
		assertThat(result.preferredRoles().get(0).asText()).isEqualTo("BACKEND");
		assertThat(result.topicOpinion()).isEqualTo("웹 서비스");
		assertThat(result.submittedAt()).isNotNull();
	}

	@Test
	@DisplayName("submit을 다시 호출하면 새 행이 아니라 기존 응답을 덮어쓴다")
	void submitOverwrites() {
		PreSurveyResponseDetailResponse first =
				preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스"));

		PreSurveyResponseDetailResponse second =
				preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("FRONTEND", "DESIGN"), "모바일 앱"));

		assertThat(second.id()).isEqualTo(first.id());
		assertThat(second.preferredRoles()).hasSize(2);
		assertThat(second.topicOpinion()).isEqualTo("모바일 앱");
	}

	@Test
	@DisplayName("getMyResponse는 본인이 제출한 응답을 돌려준다")
	void getMyResponse() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스"));

		PreSurveyResponseDetailResponse result = preSurveyResponseFacade.getMyResponse(STUDENT, SECTION_ID);

		assertThat(result.userId()).isEqualTo(STUDENT);
		assertThat(result.topicOpinion()).isEqualTo("웹 서비스");
	}

	@Test
	@DisplayName("제출한 응답이 없으면 조회는 실패한다")
	void getMyResponseNotSubmitted() {
		assertThatThrownBy(() -> preSurveyResponseFacade.getMyResponse(STUDENT, SECTION_ID))
				.isInstanceOf(PreSurveyResponseNotFoundException.class);
	}

	@Test
	@DisplayName("수강생이 아니면 제출도 조회도 막힌다")
	void rejectsNonEnrolledUser() {
		assertThatThrownBy(() -> preSurveyResponseFacade.submit(SECTION_ID, OUTSIDER, request(List.of("BACKEND"), null)))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> preSurveyResponseFacade.getMyResponse(OUTSIDER, SECTION_ID))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@DisplayName("수강 철회한 학생은 제출도 조회도 막힌다")
	void rejectsWithdrawnUser() {
		assertThatThrownBy(() -> preSurveyResponseFacade.submit(SECTION_ID, WITHDRAWN_STUDENT, request(List.of("BACKEND"), null)))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> preSurveyResponseFacade.getMyResponse(WITHDRAWN_STUDENT, SECTION_ID))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@DisplayName("조교는 제출도 조회도 막힌다")
	void rejectsAssistantUser() {
		assertThatThrownBy(() -> preSurveyResponseFacade.submit(SECTION_ID, ASSISTANT, request(List.of("BACKEND"), null)))
				.isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> preSurveyResponseFacade.getMyResponse(ASSISTANT, SECTION_ID))
				.isInstanceOf(AccessDeniedException.class);
	}
}
