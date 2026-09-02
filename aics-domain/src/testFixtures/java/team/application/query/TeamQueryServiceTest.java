package team.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;

@ExtendWith(MockitoExtension.class)
class TeamQueryServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private SectionDetail sectionDetail;

    @InjectMocks
    private TeamQueryService teamQueryService;

    @Test
    @DisplayName("getTeamsBySectionId는 분반에 속한 팀을 조회한다")
    void getTeamsBySectionId() {
        List<Team> teams = List.of(
                Team.builder().id(1L).sectionId(10L).name("1팀").build(),
                Team.builder().id(2L).sectionId(10L).name("2팀").build());
        given(sectionRepository.findById(10L)).willReturn(Optional.of(sectionDetail));
        given(teamRepository.findAllBySectionId(10L)).willReturn(teams);

        List<Team> result = teamQueryService.getTeamsBySectionId(10L);

        assertThat(result).containsExactlyElementsOf(teams);
    }

    @Test
    @DisplayName("getTeamsBySectionId는 존재하지 않는 분반이면 예외를 던진다")
    void getTeamsBySectionIdWithMissingSection() {
        given(sectionRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamQueryService.getTeamsBySectionId(10L))
                .isInstanceOf(SectionNotFoundException.class);
        verify(teamRepository, never()).findAllBySectionId(10L);
    }

    @Test
    @DisplayName("getTeamById는 식별자로 팀을 조회한다")
    void getTeamById() {
        Team team = Team.builder().id(1L).sectionId(10L).name("1팀").build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));

        Team result = teamQueryService.getTeamById(1L);

        assertThat(result).isSameAs(team);
    }

    @Test
    @DisplayName("getTeamById는 존재하지 않는 팀이면 예외를 던진다")
    void getTeamByIdWithMissingTeam() {
        given(teamRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> teamQueryService.getTeamById(1L))
                .isInstanceOf(TeamNotFoundException.class);
    }
}
