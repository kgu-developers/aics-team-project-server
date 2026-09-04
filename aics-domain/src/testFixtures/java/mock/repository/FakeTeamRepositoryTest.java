package mock.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FakeTeamRepositoryTest {

    @Test
    @DisplayName("분반 목록으로 팀 조회 시 삭제된 팀은 제외한다")
    void findAllBySectionIdInExcludesDeletedTeams() {
        FakeTeamRepository repository = new FakeTeamRepository();
        Team activeTeam = Team.create(1L, "활성 팀", null, null, Status.CONFIRMED);
        Team deletedTeam = Team.create(1L, "삭제된 팀", null, null, Status.CONFIRMED);
        deletedTeam.delete();

        repository.save(activeTeam);
        repository.save(deletedTeam);

        assertThat(repository.findAllBySectionIdIn(List.of(1L)))
            .extracting(Team::getName)
            .containsExactly("활성 팀");
    }
}
