package teamthread.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.teamthread.application.TeamThreadFacade;
import kgu.developers.api.teamthread.presentation.response.TeamThreadResponse;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import mock.repository.FakeSectionRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;
import mock.repository.FakeTeamThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

public class TeamThreadFacadeTest {

    private static final String MEMBER = "202412345";
    private static final String PROFESSOR = "P0001";
    private static final String OUTSIDER = "202400000";

    private TeamThreadFacade teamThreadFacade;

    @BeforeEach
    void init() {
        FakeTeamThreadRepository fakeTeamThreadRepository = new FakeTeamThreadRepository();
        FakeTeamRepository fakeTeamRepository = new FakeTeamRepository();
        FakeTeamMemberRepository fakeTeamMemberRepository = new FakeTeamMemberRepository();
        FakeSectionRepository fakeSectionRepository = new FakeSectionRepository();

        fakeSectionRepository.save(Section.builder().id(10L).professorId(PROFESSOR).build());
        fakeTeamRepository.save(Team.builder().id(1L).sectionId(10L).status(Status.CONFIRMED).build());
        fakeTeamMemberRepository.save(TeamMember.create(1L, MEMBER, false, "기록자"));

        teamThreadFacade = new TeamThreadFacade(
            new TeamThreadCommandService(fakeTeamThreadRepository),
            new TeamAccessValidator(fakeTeamRepository, fakeTeamMemberRepository, fakeSectionRepository)
        );
    }

    @Test
    @DisplayName("getOrCreateThread는 스레드가 없는 팀이면 새로 생성하여 반환한다")
    void getOrCreateThread_ThreadNotExists_CreatesNewThread() {
        // given
        Long teamId = 1L;

        // when
        TeamThreadResponse result = teamThreadFacade.getOrCreateThread(teamId, MEMBER);

        // then
        assertEquals(teamId, result.teamId());
        assertNotNull(result.threadId());
    }

    @Test
    @DisplayName("getOrCreateThread는 이미 존재하는 스레드를 새로 만들지 않고 재사용한다")
    void getOrCreateThread_ThreadExists_ReusesExistingThread() {
        // given
        Long teamId = 1L;
        TeamThreadResponse first = teamThreadFacade.getOrCreateThread(teamId, MEMBER);

        // when
        TeamThreadResponse second = teamThreadFacade.getOrCreateThread(teamId, MEMBER);

        // then
        assertEquals(first.threadId(), second.threadId());
    }

    @Test
    @DisplayName("getOrCreateThread는 담당 교수도 접근할 수 있다")
    void getOrCreateThread_Professor_Allowed() {
        // when
        TeamThreadResponse result = teamThreadFacade.getOrCreateThread(1L, PROFESSOR);

        // then
        assertEquals(1L, result.teamId());
    }

    @Test
    @DisplayName("getOrCreateThread는 팀 소속도 담당 교수도 아니면 접근이 거부된다")
    void getOrCreateThread_Outsider_ThrowsAccessDenied() {
        // when & then
        assertThatThrownBy(() -> teamThreadFacade.getOrCreateThread(1L, OUTSIDER))
            .isInstanceOf(AccessDeniedException.class);
    }
}
