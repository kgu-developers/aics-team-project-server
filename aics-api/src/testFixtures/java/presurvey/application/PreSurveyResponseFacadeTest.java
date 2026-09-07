package presurvey.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;

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
import kgu.developers.domain.enrollment.application.query.EnrollmentQueryService;
import kgu.developers.domain.notification.application.command.NotificationOutboxService;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.application.query.PreSurveyResponseQueryService;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponseNotFoundException;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import mock.repository.FakePreSurveyResponseRepository;
import mock.repository.FakeUserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PreSurveyResponseFacadeTest {

	private static final Long SECTION_ID = 1L;
	private static final String STUDENT = "202012345";
	private static final String OUTSIDER = "202000000";
	private static final String WITHDRAWN_STUDENT = "202099999";
	private static final String ASSISTANT = "202088888";
	private static final String PEER = "202054321";
	private static final String OTHER_PEER = "202011111";

	private static final String STUDENT_NAME = "김철수";
	private static final String PEER_NAME = "이영희";

	@Mock
	private EnrollmentRepository enrollmentRepository;

	@Mock
	private EnrollmentQueryService enrollmentQueryService;

	@Mock
	private NotificationOutboxService notificationOutboxService;

	@Mock
	private UserQueryService userQueryService;

	@Mock
	private UserQueryService commandUserQueryService;

	private PreSurveyResponseFacade preSurveyResponseFacade;

	@BeforeEach
	void init() {
		FakePreSurveyResponseRepository repository = new FakePreSurveyResponseRepository();
		preSurveyResponseFacade = new PreSurveyResponseFacade(
				new PreSurveyResponseCommandService(repository, enrollmentRepository, notificationOutboxService, commandUserQueryService),
				new PreSurveyResponseQueryService(repository, enrollmentRepository, new FakeUserRepository()),
				enrollmentRepository,
				enrollmentQueryService,
				userQueryService
		);
		given(userQueryService.getUserByStudentNumber(STUDENT))
				.willReturn(User.create(STUDENT, "student@kyonggi.ac.kr", STUDENT_NAME, "password", UserGlobalRole.USER, null));
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
		// 지목 대상·수락자로 쓰는 같은 분반 학생들
		for (String peer : List.of(PEER, OTHER_PEER)) {
			given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, peer))
					.willReturn(Optional.of(Enrollment.create(SECTION_ID, peer, Role.STUDENT, Status.ACTIVE)));
			given(enrollmentRepository.findBySectionIdAndUserIdForUpdate(SECTION_ID, peer))
					.willReturn(Optional.of(Enrollment.create(SECTION_ID, peer, Role.STUDENT, Status.ACTIVE)));
		}
		given(userQueryService.getUserByStudentNumber(PEER))
				.willReturn(User.create(PEER, "peer@kyonggi.ac.kr", PEER_NAME, "password", UserGlobalRole.USER, null));
		// Stub for the command service's user query service
		given(commandUserQueryService.getUserByStudentNumber(STUDENT))
				.willReturn(User.create(STUDENT, "student@kyonggi.ac.kr", STUDENT_NAME, "password", UserGlobalRole.USER, null));
		given(commandUserQueryService.getUserByStudentNumber(PEER))
				.willReturn(User.create(PEER, "peer@kyonggi.ac.kr", PEER_NAME, "password", UserGlobalRole.USER, null));
		given(commandUserQueryService.getUserByStudentNumber(OTHER_PEER))
				.willReturn(User.create(OTHER_PEER, "other@kyonggi.ac.kr", "Other", "password", UserGlobalRole.USER, null));
	}

	private PreSurveyResponseSubmitRequest request(List<String> roles, String topicOpinion) {
		return request(roles, topicOpinion, null);
	}

	private PreSurveyResponseSubmitRequest request(List<String> roles, String topicOpinion, String preferredPeerUserId) {
		return PreSurveyResponseSubmitRequest.builder()
				.preferredRoles(roles)
				.topicOpinion(topicOpinion)
				.etcOpinion("없음")
				.preferredPeerUserId(preferredPeerUserId)
				.build();
	}

	@Test
	@DisplayName("submit은 희망 역할을 JSON 배열로 저장한 응답을 돌려준다")
	void submit() {
		PreSurveyResponseDetailResponse result =
				preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND", "PM"), "웹 서비스"));

		assertThat(result.id()).isNotNull();
		assertThat(result.userId()).isEqualTo(STUDENT);
		assertThat(result.userName()).isEqualTo(STUDENT_NAME);
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
		assertThat(result.userName()).isEqualTo(STUDENT_NAME);
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

	@Test
	@DisplayName("새로 지목하면 지목당한 학생에게 알림이 간다")
	void submitNotifiesPreferredPeer() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));

		then(notificationOutboxService).should().createNotificationOutbox(
				eq(PEER),
				eq(NotificationType.PRE_SURVEY_PREFERRED_PEER_REQUESTED),
				any(),
				eq("조원 지목 요청"),
				eq(STUDENT_NAME + " 님이 회원님을 조원으로 지목했습니다."),
				isNull());
	}

	@Test
	@DisplayName("같은 학생을 지목한 채 의견만 고쳐 재제출하면 알림이 다시 가지 않는다")
	void resubmitWithSamePeerDoesNotNotifyAgain() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));
		clearInvocations(notificationOutboxService);

		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("FRONTEND"), "주제 바꿈", PEER));

		then(notificationOutboxService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("지목 대상을 바꾸면 새 대상에게는 요청, 이전 대상에게는 취소 알림이 간다")
	void changingPeerNotifiesBothSides() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));
		clearInvocations(notificationOutboxService);

		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", OTHER_PEER));

		then(notificationOutboxService).should().createNotificationOutbox(
				eq(OTHER_PEER), eq(NotificationType.PRE_SURVEY_PREFERRED_PEER_REQUESTED),
				any(), any(), any(), isNull());
		then(notificationOutboxService).should().createNotificationOutbox(
				eq(PEER), eq(NotificationType.PRE_SURVEY_PREFERRED_PEER_CANCELLED),
				any(), eq("조원 지목 취소"), any(), isNull());
	}

	@Test
	@DisplayName("지목을 취소하면(대상 null) 지목당했던 학생에게 취소 알림이 간다")
	void cancellingPeerNotifiesPreviousPeer() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));
		clearInvocations(notificationOutboxService);

		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", null));

		then(notificationOutboxService).should().createNotificationOutbox(
				eq(PEER),
				eq(NotificationType.PRE_SURVEY_PREFERRED_PEER_CANCELLED),
				any(),
				eq("조원 지목 취소"),
				eq(STUDENT_NAME + " 님이 회원님에 대한 조원 지목을 취소했습니다."),
				isNull());
	}

	@Test
	@DisplayName("이미 거절한 상대에게는 취소 알림을 보내지 않는다")
	void cancellingAfterRejectionDoesNotNotify() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));
		preSurveyResponseFacade.decidePreferredPeer(PEER, SECTION_ID, STUDENT, false);
		clearInvocations(notificationOutboxService);

		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", null));

		then(notificationOutboxService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("수락한 상대에게는 취소 알림을 보낸다")
	void cancellingAfterAcceptanceNotifies() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));
		preSurveyResponseFacade.decidePreferredPeer(PEER, SECTION_ID, STUDENT, true);
		clearInvocations(notificationOutboxService);

		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", null));

		then(notificationOutboxService).should().createNotificationOutbox(
				eq(PEER), eq(NotificationType.PRE_SURVEY_PREFERRED_PEER_CANCELLED),
				any(), eq("조원 지목 취소"), any(), isNull());
	}

	@Test
	@DisplayName("수락하면 지목한 학생에게 수락 알림이 간다")
	void acceptNotifiesRequester() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));
		clearInvocations(notificationOutboxService);

		preSurveyResponseFacade.decidePreferredPeer(PEER, SECTION_ID, STUDENT, true);

		then(notificationOutboxService).should().createNotificationOutbox(
				eq(STUDENT),
				eq(NotificationType.PRE_SURVEY_PREFERRED_PEER_DECIDED),
				any(),
				eq("조원 지목 수락"),
				eq(PEER_NAME + " 님이 회원님의 조원 지목을 수락했습니다."),
				isNull());
	}

	@Test
	@DisplayName("거절하면 지목한 학생에게 거절 알림이 간다")
	void rejectNotifiesRequester() {
		preSurveyResponseFacade.submit(SECTION_ID, STUDENT, request(List.of("BACKEND"), "웹 서비스", PEER));
		clearInvocations(notificationOutboxService);

		preSurveyResponseFacade.decidePreferredPeer(PEER, SECTION_ID, STUDENT, false);

		then(notificationOutboxService).should().createNotificationOutbox(
				eq(STUDENT),
				eq(NotificationType.PRE_SURVEY_PREFERRED_PEER_DECIDED),
				any(),
				eq("조원 지목 거절"),
				eq(PEER_NAME + " 님이 회원님의 조원 지목을 거절했습니다."),
				isNull());
	}
}
