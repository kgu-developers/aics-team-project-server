package teammessage.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.teammessage.application.TeamMessageFacade;
import kgu.developers.api.teammessage.presentation.request.TeamMessageCreateRequest;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePageResponse;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePersistResponse;
import kgu.developers.api.teammessage.presentation.response.UnreadMessageCountResponse;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import mock.repository.FakeSectionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamMessageReadReceiptRepository;
import mock.repository.FakeTeamMessageRepository;
import mock.repository.FakeTeamRepository;
import mock.repository.FakeTeamThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TeamMessageFacadeTest {

    private static final String USER_A = "202412345";
    private static final String USER_B = "202412346";
    private static final String OUTSIDER = "202400000";
    private static final String PROFESSOR_ID = "202699999";

    private TeamMessageFacade teamMessageFacade;
    private FakeTeamThreadRepository fakeTeamThreadRepository;
    private FakeTeamRepository fakeTeamRepository;
    private FakeSectionRepository fakeSectionRepository;

    @BeforeEach
    void init() {
        fakeTeamThreadRepository = new FakeTeamThreadRepository();
        FakeTeamMessageRepository fakeTeamMessageRepository = new FakeTeamMessageRepository();
        FakeTeamMessageReadReceiptRepository fakeTeamMessageReadReceiptRepository = new FakeTeamMessageReadReceiptRepository();
        FakeTeamMemberRepository fakeTeamMemberRepository = new FakeTeamMemberRepository();
        fakeTeamRepository = new FakeTeamRepository();
        fakeSectionRepository = new FakeSectionRepository();

        fakeTeamMemberRepository.save(TeamMember.create(1L, USER_A, false, "기록자"));
        fakeTeamMemberRepository.save(TeamMember.create(1L, USER_B, false, "발표자"));
        fakeTeamMemberRepository.save(TeamMember.create(2L, USER_A, false, "기록자"));
        fakeTeamMemberRepository.save(TeamMember.create(99L, USER_A, false, "기록자"));

        teamMessageFacade = new TeamMessageFacade(
            new TeamThreadCommandService(fakeTeamThreadRepository),
            new TeamThreadQueryService(fakeTeamThreadRepository),
            new TeamMessageCommandService(fakeTeamMessageRepository, fakeTeamMessageReadReceiptRepository),
            new TeamMessageQueryService(fakeTeamMessageRepository, fakeTeamMessageReadReceiptRepository),
            new TeamAccessValidator(fakeTeamRepository, fakeTeamMemberRepository, fakeSectionRepository)
        );
    }

    private TeamMessageCreateRequest createRequest(String message) {
        return TeamMessageCreateRequest.builder()
            .message(message)
            .build();
    }

    @Test
    @DisplayName("postMessage는 relatedType을 지정하지 않으면 GENERAL로 기본 설정하여 메시지를 등록한다")
    void postMessage_RelatedTypeOmitted_DefaultsToGeneral() {
        // given
        Long teamId = 1L;
        TeamMessageCreateRequest request = createRequest("다음 회의 일정 문의드립니다.");

        // when
        TeamMessagePersistResponse result = teamMessageFacade.postMessage(teamId, USER_A, request);

        // then
        assertEquals(TeamMessageRelatedType.GENERAL, result.relatedType());
        assertEquals(USER_A, result.senderId());
    }

    @Test
    @DisplayName("postMessage는 스레드가 없는 팀이어도 스레드를 지연 생성하며 메시지를 등록한다")
    void postMessage_ThreadNotExists_CreatesThreadLazily() {
        // given
        Long teamId = 2L;
        TeamMessageCreateRequest request = TeamMessageCreateRequest.builder()
            .relatedType(TeamMessageRelatedType.QUESTION)
            .message("질문 있습니다.")
            .build();

        // when
        TeamMessagePersistResponse result = teamMessageFacade.postMessage(teamId, USER_A, request);

        // then
        assertNotNull(result.id());
        assertTrue(fakeTeamThreadRepository.findByTeamId(teamId).isPresent());
    }

    @Test
    @DisplayName("getMessages는 스레드가 없는 팀을 조회하면 예외를 던진다")
    void getMessages_ThreadNotExists_ThrowsException() {
        // given
        Long teamId = 99L;

        // when & then
        assertThatThrownBy(() -> teamMessageFacade.getMessages(teamId, null, PageRequest.of(0, 10), USER_A))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateImportant는 메시지의 중요 표시 여부를 변경한다")
    void updateImportant_Success() {
        // given
        Long teamId = 1L;
        TeamMessagePersistResponse posted = teamMessageFacade.postMessage(teamId, USER_A, createRequest("내용"));

        // when
        teamMessageFacade.updateImportant(posted.id(), true, USER_A);

        // then
        TeamMessagePageResponse messages = teamMessageFacade.getMessages(teamId, null, PageRequest.of(0, 10), USER_A);
        assertTrue(messages.contents().get(0).important());
    }

    @Test
    @DisplayName("postMessage/getMessages/updateImportant/markAsRead는 팀 소속이 아니면 접근이 거부된다")
    void teamOperations_NonMember_ThrowsAccessDenied() {
        // given
        Long teamId = 1L;
        TeamMessagePersistResponse posted = teamMessageFacade.postMessage(teamId, USER_A, createRequest("내용"));

        // when & then
        assertThatThrownBy(() -> teamMessageFacade.postMessage(teamId, OUTSIDER, createRequest("침입")))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> teamMessageFacade.getMessages(teamId, null, PageRequest.of(0, 10), OUTSIDER))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> teamMessageFacade.updateImportant(posted.id(), true, OUTSIDER))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> teamMessageFacade.markAsRead(posted.id(), OUTSIDER))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> teamMessageFacade.getUnreadCount(teamId, OUTSIDER))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("markAsRead는 메시지를 읽음 처리한 사용자의 읽지 않은 메시지 수에서만 제외시킨다")
    void markAsRead_OnlyAffectsReadingUsersUnreadCount() {
        // given
        Long teamId = 1L;
        TeamMessagePersistResponse posted = teamMessageFacade.postMessage(teamId, USER_A, createRequest("내용"));

        // when
        teamMessageFacade.markAsRead(posted.id(), USER_A);

        // then
        assertEquals(0, teamMessageFacade.getUnreadCount(teamId, USER_A).count());
        assertEquals(1, teamMessageFacade.getUnreadCount(teamId, USER_B).count());
    }

    @Test
    @DisplayName("getMessages의 read 값은 요청한 사용자가 그 메시지를 읽었는지만 나타낸다")
    void getMessages_ReadFlagIsPerRequestingUser() {
        // given
        Long teamId = 1L;
        TeamMessagePersistResponse posted = teamMessageFacade.postMessage(teamId, USER_A, createRequest("내용"));
        teamMessageFacade.markAsRead(posted.id(), USER_A);

        // when
        boolean readByUserA = teamMessageFacade.getMessages(teamId, null, PageRequest.of(0, 10), USER_A)
            .contents().get(0).read();
        boolean readByUserB = teamMessageFacade.getMessages(teamId, null, PageRequest.of(0, 10), USER_B)
            .contents().get(0).read();

        // then
        assertTrue(readByUserA);
        assertFalse(readByUserB);
    }

    @Test
    @DisplayName("getUnreadCount는 읽지 않은 메시지 수를 정확히 반환한다")
    void getUnreadCount_ReturnsCorrectCount() {
        // given
        Long teamId = 1L;
        teamMessageFacade.postMessage(teamId, USER_A, createRequest("1"));
        teamMessageFacade.postMessage(teamId, USER_A, createRequest("2"));

        // when
        UnreadMessageCountResponse count = teamMessageFacade.getUnreadCount(teamId, USER_A);

        // then
        assertEquals(2, count.count());
    }

    @Test
    @DisplayName("getMessages는 relatedType으로 필터링하여 메시지 목록을 페이지 단위로 조회한다")
    void getMessages_FiltersByRelatedType() {
        // given
        Long teamId = 1L;
        teamMessageFacade.postMessage(teamId, USER_A, TeamMessageCreateRequest.builder()
            .relatedType(TeamMessageRelatedType.QUESTION).message("질문").build());
        teamMessageFacade.postMessage(teamId, USER_A, createRequest("일반 메시지"));

        // when
        TeamMessagePageResponse result = teamMessageFacade.getMessages(teamId, TeamMessageRelatedType.QUESTION, PageRequest.of(0, 10), USER_A);

        // then
        assertEquals(1, result.contents().size());
        assertEquals(TeamMessageRelatedType.QUESTION, result.contents().get(0).relatedType());
    }

    @Test
    @DisplayName("담당 교수는 팀 메시지를 조회·등록하고 중요 표시·읽음 처리를 할 수 있다")
    void teamOperations_ProfessorCanAccessOwnedSectionTeam() {
        // given
        fakeSectionRepository.save(Section.builder()
            .id(1L)
            .professorId(PROFESSOR_ID)
            .build());
        fakeTeamRepository.save(Team.builder()
            .id(1L)
            .sectionId(1L)
            .name("A팀")
            .kickoffRule("규칙")
            .meetingSchedule("매주 월요일")
            .status(Status.CONFIRMED)
            .build());
        TeamMessagePersistResponse posted = teamMessageFacade.postMessage(1L, USER_A, createRequest("확인 부탁드립니다."));

        // when
        TeamMessagePageResponse messages = teamMessageFacade.getMessages(
            1L, null, PageRequest.of(0, 10), PROFESSOR_ID);
        teamMessageFacade.updateImportant(posted.id(), true, PROFESSOR_ID);
        teamMessageFacade.markAsRead(posted.id(), PROFESSOR_ID);
        UnreadMessageCountResponse unreadCount = teamMessageFacade.getUnreadCount(1L, PROFESSOR_ID);
        TeamMessagePersistResponse reply = teamMessageFacade.postMessage(
            1L, PROFESSOR_ID, createRequest("확인했습니다."));

        // then
        assertEquals(1, messages.contents().size());
        assertEquals(0, unreadCount.count());
        assertEquals(PROFESSOR_ID, reply.senderId());
        assertTrue(teamMessageFacade.getMessages(1L, null, PageRequest.of(0, 10), PROFESSOR_ID)
            .contents().stream()
            .filter(message -> message.id().equals(posted.id()))
            .findFirst()
            .orElseThrow()
            .important());
    }
}
