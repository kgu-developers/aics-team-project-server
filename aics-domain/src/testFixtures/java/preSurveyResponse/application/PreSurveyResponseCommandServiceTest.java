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
import kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException;
import kgu.developers.domain.preSurveyResponse.application.command.PreSurveyResponseCommandService;
import kgu.developers.domain.preSurveyResponse.domain.PreSurveyResponse;
import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredPeerInvalidException;
import kgu.developers.domain.preSurveyResponse.exception.PreSurveyResponsePreferredPeerRequestNotFoundException;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeNotificationOutboxService;
import mock.repository.FakePreSurveyResponseRepository;
import mock.repository.FakeUserQueryService;

class PreSurveyResponseCommandServiceTest {

	private static final String USER_ID = "202012345";
	private static final String PEER_ID = "202054321";
	private static final Long SECTION_ID = 1L;

	private FakePreSurveyResponseRepository repository;
	private FakeEnrollmentRepository enrollmentRepository;
	private FakeNotificationOutboxService notificationOutboxService;
	private FakeUserQueryService userQueryService;
	private PreSurveyResponseCommandService commandService;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		repository = new FakePreSurveyResponseRepository();
		enrollmentRepository = new FakeEnrollmentRepository();
		notificationOutboxService = new FakeNotificationOutboxService();
		userQueryService = new FakeUserQueryService();
		enrollmentRepository.save(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE));
		userQueryService.save(kgu.developers.domain.user.domain.User.create(USER_ID, "user@test.com", "Test User", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
		commandService = new PreSurveyResponseCommandService(repository, enrollmentRepository, notificationOutboxService, userQueryService);
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
		PreSurveyResponse response = commandService.submit(userId, sectionId, roles, "웹 개발", "특이사항 없음", null);

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
		PreSurveyResponse initial = commandService.submit(userId, sectionId, initialRoles, "웹 개발", "없음", null);

		// when
		JsonNode updatedRoles = objectMapper.readTree("[\"FRONTEND\", \"DESIGN\"]");
		PreSurveyResponse updated = commandService.submit(userId, sectionId, updatedRoles, "앱 개발", "수정됨", null);

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
				"202099999", SECTION_ID, roles, "웹 개발", "없음", null))
				.isInstanceOf(kgu.developers.domain.enrollment.exception.EnrollmentNotFoundException.class);
	}

	@Test
	@DisplayName("본인이거나 같은 분반 수강생이 아닌 학생은 조원으로 지목할 수 없다")
	void submit_RejectsInvalidPreferredPeer() throws Exception {
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");

		assertThatThrownBy(() -> commandService.submit(USER_ID, SECTION_ID, roles, null, null, USER_ID))
				.isInstanceOf(PreSurveyResponsePreferredPeerInvalidException.class);
		assertThatThrownBy(() -> commandService.submit(USER_ID, SECTION_ID, roles, null, null, "202099999"))
				.isInstanceOf(PreSurveyResponsePreferredPeerInvalidException.class);
	}

	@Test
	@DisplayName("수강 철회한 학생은 조원으로 지목할 수 없다")
	void submit_RejectsWithdrawnPreferredPeer() throws Exception {
		enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER_ID, Role.STUDENT, Status.WITHDRAWN));
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");

		assertThatThrownBy(() -> commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID))
				.isInstanceOf(PreSurveyResponsePreferredPeerInvalidException.class);
	}

	@Test
	@DisplayName("조교는 조원으로 지목할 수 없다")
	void submit_RejectsAssistantPreferredPeer() throws Exception {
		enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER_ID, Role.ASSISTANT, Status.ACTIVE));
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");

		assertThatThrownBy(() -> commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID))
				.isInstanceOf(PreSurveyResponsePreferredPeerInvalidException.class);
	}

	@Test
	@DisplayName("지목 대상이 나중에 수강 철회해도 의견만 고치는 재제출은 통과한다")
	void submit_AllowsResubmitAfterPeerWithdraws() throws Exception {
		Enrollment peer = enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER_ID, Role.STUDENT, Status.ACTIVE));
		userQueryService.save(kgu.developers.domain.user.domain.User.create(PEER_ID, "peer@test.com", "Peer User", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
		commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID);

		withdraw(peer);

		PreSurveyResponse resubmitted = commandService.submit(USER_ID, SECTION_ID, roles, "의견 수정", null, PEER_ID);

		assertThat(resubmitted.getTopicOpinion()).isEqualTo("의견 수정");
		assertThat(resubmitted.getPreferredPeerUserId()).isEqualTo(PEER_ID);
	}

	@Test
	@DisplayName("철회한 학생을 지목한 채로도 다른 학생으로 대상을 바꾸는 것은 검증을 탄다")
	void submit_StillValidatesWhenPeerChangesAfterWithdrawal() throws Exception {
		Enrollment peer = enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER_ID, Role.STUDENT, Status.ACTIVE));
		userQueryService.save(kgu.developers.domain.user.domain.User.create(PEER_ID, "peer@test.com", "Peer User", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
		commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID);
		withdraw(peer);

		// 대상이 바뀌면 새 대상은 검증한다
		assertThatThrownBy(() -> commandService.submit(USER_ID, SECTION_ID, roles, null, null, "202099999"))
				.isInstanceOf(PreSurveyResponsePreferredPeerInvalidException.class);
		// 지목을 푸는 것은 언제나 된다
		assertThat(commandService.submit(USER_ID, SECTION_ID, roles, null, null, null).getPreferredPeerUserId())
				.isNull();
	}

	/** Fake 는 id 로 덮어쓰므로 같은 id 로 저장해야 상태 변경이 된다. Enrollment.create 는 새 id 를 받아 행이 하나 더 생긴다. */
	private void withdraw(Enrollment enrollment) {
		enrollmentRepository.save(Enrollment.builder()
				.id(enrollment.getId())
				.sectionId(enrollment.getSectionId())
				.userId(enrollment.getUserId())
				.role(enrollment.getRole())
				.status(Status.WITHDRAWN)
				.createdAt(enrollment.getCreatedAt())
				.build());
	}

	@Test
	@DisplayName("지목당한 학생이 수락하면 지목한 쪽 응답의 상태가 ACCEPTED가 된다")
	void decidePreferredPeer_Accept() throws Exception {
		enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER_ID, Role.STUDENT, Status.ACTIVE));
		userQueryService.save(kgu.developers.domain.user.domain.User.create(PEER_ID, "peer@test.com", "Peer User", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
		commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID);

		PreSurveyResponse decided = commandService.decidePreferredPeer(PEER_ID, SECTION_ID, USER_ID, true);

		assertThat(decided.getPreferredPeerStatus()).isEqualTo(PreferredPeerStatus.ACCEPTED);
	}

	@Test
	@DisplayName("수강 철회한 학생의 재제출은 거부한다")
	void submit_RejectsWithdrawnSubmitter() throws Exception {
		Enrollment me = enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID).orElseThrow();
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
		commandService.submit(USER_ID, SECTION_ID, roles, null, null, null);

		withdraw(me);

		assertThatThrownBy(() -> commandService.submit(USER_ID, SECTION_ID, roles, "의견 수정", null, null))
				.isInstanceOf(EnrollmentNotFoundException.class);
	}

	@Test
	@DisplayName("지목한 학생이 수강 철회했으면 그 지목은 수락·거절할 수 없다")
	void decidePreferredPeer_RejectsWithdrawnRequester() throws Exception {
		Enrollment me = enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID).orElseThrow();
		savePeer(Role.STUDENT);
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
		commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID);

		withdraw(me);

		assertThatThrownBy(() -> commandService.decidePreferredPeer(PEER_ID, SECTION_ID, USER_ID, true))
				.isInstanceOf(PreSurveyResponsePreferredPeerRequestNotFoundException.class);
	}

	@Test
	@DisplayName("수강 철회했거나 조교가 된 학생은 자기가 받은 지목을 수락·거절할 수 없다")
	void decidePreferredPeer_RejectsInactivePeer() throws Exception {
		Enrollment peer = savePeer(Role.STUDENT);
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
		commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID);

		withdraw(peer);

		assertThatThrownBy(() -> commandService.decidePreferredPeer(PEER_ID, SECTION_ID, USER_ID, true))
				.isInstanceOf(PreSurveyResponsePreferredPeerRequestNotFoundException.class);
	}

	private Enrollment savePeer(Role role) {
		userQueryService.save(kgu.developers.domain.user.domain.User.create(PEER_ID, "peer@test.com", "Peer User", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
		return enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER_ID, role, Status.ACTIVE));
	}

	@Test
	@DisplayName("이미 처리했거나 나를 지목하지 않은 신청은 수락·거절할 수 없다")
	void decidePreferredPeer_RejectsNonPending() throws Exception {
		enrollmentRepository.save(Enrollment.create(SECTION_ID, PEER_ID, Role.STUDENT, Status.ACTIVE));
		userQueryService.save(kgu.developers.domain.user.domain.User.create(PEER_ID, "peer@test.com", "Peer User", "password", kgu.developers.domain.user.domain.UserGlobalRole.USER, null));
		JsonNode roles = objectMapper.readTree("[\"BACKEND\"]");
		commandService.submit(USER_ID, SECTION_ID, roles, null, null, PEER_ID);
		commandService.decidePreferredPeer(PEER_ID, SECTION_ID, USER_ID, false);

		// 이미 거절한 신청
		assertThatThrownBy(() -> commandService.decidePreferredPeer(PEER_ID, SECTION_ID, USER_ID, true))
				.isInstanceOf(PreSurveyResponsePreferredPeerRequestNotFoundException.class);
		// 나를 지목하지 않은 응답
		assertThatThrownBy(() -> commandService.decidePreferredPeer("202077777", SECTION_ID, USER_ID, true))
				.isInstanceOf(PreSurveyResponsePreferredPeerRequestNotFoundException.class);
	}
}
