package teamthread.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import kgu.developers.api.teamthread.application.TeamThreadFacade;
import kgu.developers.api.teamthread.presentation.response.TeamThreadResponse;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import mock.repository.FakeTeamThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TeamThreadFacadeTest {

    private TeamThreadFacade teamThreadFacade;

    @BeforeEach
    void init() {
        FakeTeamThreadRepository fakeTeamThreadRepository = new FakeTeamThreadRepository();
        teamThreadFacade = new TeamThreadFacade(new TeamThreadCommandService(fakeTeamThreadRepository));
    }

    @Test
    @DisplayName("getOrCreateThread는 스레드가 없는 팀이면 새로 생성하여 반환한다")
    void getOrCreateThread_ThreadNotExists_CreatesNewThread() {
        // given
        Long teamId = 1L;

        // when
        TeamThreadResponse result = teamThreadFacade.getOrCreateThread(teamId);

        // then
        assertEquals(teamId, result.teamId());
        assertNotNull(result.threadId());
    }

    @Test
    @DisplayName("getOrCreateThread는 이미 존재하는 스레드를 새로 만들지 않고 재사용한다")
    void getOrCreateThread_ThreadExists_ReusesExistingThread() {
        // given
        Long teamId = 1L;
        TeamThreadResponse first = teamThreadFacade.getOrCreateThread(teamId);

        // when
        TeamThreadResponse second = teamThreadFacade.getOrCreateThread(teamId);

        // then
        assertEquals(first.threadId(), second.threadId());
    }
}
