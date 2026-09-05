package teammessage.presentation.response;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminPageResponse;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.exception.TeamThreadNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class TeamMessageAdminPageResponseTest {

    private static final Long THREAD_ID = 100L;
    private static final Long TEAM_ID = 10L;
    private static final Long SECTION_ID = 1L;

    @Test
    @DisplayName("메시지의 스레드가 조회 결과에 없으면 명시적인 예외를 던진다")
    void from_MissingThread() {
        assertThatThrownBy(() -> createResponse(Map.of(), Map.of(), Map.of()))
            .isInstanceOf(TeamThreadNotFoundException.class);
    }

    @Test
    @DisplayName("스레드의 팀이 조회 결과에 없으면 명시적인 예외를 던진다")
    void from_MissingTeam() {
        Map<Long, TeamThread> threadsById = Map.of(
            THREAD_ID, TeamThread.builder().id(THREAD_ID).teamId(TEAM_ID).build());

        assertThatThrownBy(() -> createResponse(threadsById, Map.of(), Map.of()))
            .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    @DisplayName("팀의 분반이 조회 결과에 없으면 명시적인 예외를 던진다")
    void from_MissingSection() {
        Map<Long, TeamThread> threadsById = Map.of(
            THREAD_ID, TeamThread.builder().id(THREAD_ID).teamId(TEAM_ID).build());
        Map<Long, Team> teamsById = Map.of(
            TEAM_ID, Team.builder().id(TEAM_ID).sectionId(SECTION_ID).build());

        assertThatThrownBy(() -> createResponse(threadsById, teamsById, Map.of()))
            .isInstanceOf(SectionNotFoundException.class);
    }

    private void createResponse(
        Map<Long, TeamThread> threadsById,
        Map<Long, Team> teamsById,
        Map<Long, Section> sectionsById
    ) {
        TeamMessage message = TeamMessage.builder()
            .id(1000L)
            .threadId(THREAD_ID)
            .build();

        TeamMessageAdminPageResponse.from(
            new PageImpl<>(List.of(message)),
            0L,
            Set.of(),
            threadsById,
            teamsById,
            sectionsById
        );
    }
}
