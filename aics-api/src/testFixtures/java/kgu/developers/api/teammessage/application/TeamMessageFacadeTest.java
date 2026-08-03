package kgu.developers.api.teammessage.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kgu.developers.api.teammessage.presentation.request.TeamMessageCreateRequest;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePageResponse;
import kgu.developers.api.teammessage.presentation.response.TeamMessagePersistResponse;
import kgu.developers.api.teammessage.presentation.response.UnreadMessageCountResponse;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.FakeTeamMessageRepository;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.FakeTeamThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

public class TeamMessageFacadeTest {

    private TeamMessageFacade teamMessageFacade;
    private FakeTeamThreadRepository fakeTeamThreadRepository;

    @BeforeEach
    void init() {
        fakeTeamThreadRepository = new FakeTeamThreadRepository();
        FakeTeamMessageRepository fakeTeamMessageRepository = new FakeTeamMessageRepository();

        teamMessageFacade = new TeamMessageFacade(
            new TeamThreadCommandService(fakeTeamThreadRepository),
            new TeamThreadQueryService(fakeTeamThreadRepository),
            new TeamMessageCommandService(fakeTeamMessageRepository),
            new TeamMessageQueryService(fakeTeamMessageRepository)
        );
    }

    private TeamMessageCreateRequest createRequest(String message) {
        return TeamMessageCreateRequest.builder()
            .senderId("202412345")
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
        TeamMessagePersistResponse result = teamMessageFacade.postMessage(teamId, request);

        // then
        assertEquals(TeamMessageRelatedType.GENERAL, result.relatedType());
    }

    @Test
    @DisplayName("postMessage는 스레드가 없는 팀이어도 스레드를 지연 생성하며 메시지를 등록한다")
    void postMessage_ThreadNotExists_CreatesThreadLazily() {
        // given
        Long teamId = 2L;
        TeamMessageCreateRequest request = TeamMessageCreateRequest.builder()
            .senderId("202412345")
            .relatedType(TeamMessageRelatedType.QUESTION)
            .message("질문 있습니다.")
            .build();

        // when
        TeamMessagePersistResponse result = teamMessageFacade.postMessage(teamId, request);

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
        assertThatThrownBy(() -> teamMessageFacade.getMessages(teamId, null, PageRequest.of(0, 10)))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateImportant는 메시지의 중요 표시 여부를 변경한다")
    void updateImportant_Success() {
        // given
        Long teamId = 1L;
        TeamMessagePersistResponse posted = teamMessageFacade.postMessage(teamId, createRequest("내용"));

        // when
        teamMessageFacade.updateImportant(posted.id(), true);

        // then
        TeamMessagePageResponse messages = teamMessageFacade.getMessages(teamId, null, PageRequest.of(0, 10));
        assertTrue(messages.contents().get(0).important());
    }

    @Test
    @DisplayName("markAsRead는 메시지를 읽음 처리하여 읽지 않은 메시지 수에서 제외시킨다")
    void markAsRead_Success() {
        // given
        Long teamId = 1L;
        TeamMessagePersistResponse posted = teamMessageFacade.postMessage(teamId, createRequest("내용"));

        // when
        teamMessageFacade.markAsRead(posted.id());

        // then
        UnreadMessageCountResponse count = teamMessageFacade.getUnreadCount(teamId);
        assertEquals(0, count.count());
    }

    @Test
    @DisplayName("getUnreadCount는 읽지 않은 메시지 수를 정확히 반환한다")
    void getUnreadCount_ReturnsCorrectCount() {
        // given
        Long teamId = 1L;
        teamMessageFacade.postMessage(teamId, createRequest("1"));
        teamMessageFacade.postMessage(teamId, createRequest("2"));

        // when
        UnreadMessageCountResponse count = teamMessageFacade.getUnreadCount(teamId);

        // then
        assertEquals(2, count.count());
    }

    @Test
    @DisplayName("getMessages는 relatedType으로 필터링하여 메시지 목록을 페이지 단위로 조회한다")
    void getMessages_FiltersByRelatedType() {
        // given
        Long teamId = 1L;
        teamMessageFacade.postMessage(teamId, TeamMessageCreateRequest.builder()
            .senderId("202412345").relatedType(TeamMessageRelatedType.QUESTION).message("질문").build());
        teamMessageFacade.postMessage(teamId, createRequest("일반 메시지"));

        // when
        TeamMessagePageResponse result = teamMessageFacade.getMessages(teamId, TeamMessageRelatedType.QUESTION, PageRequest.of(0, 10));

        // then
        assertEquals(1, result.contents().size());
        assertEquals(TeamMessageRelatedType.QUESTION, result.contents().get(0).relatedType());
    }
}
