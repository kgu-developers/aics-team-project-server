package team.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.DuplicateTeamNameException;
import kgu.developers.domain.team.exception.TeamAlreadyConfirmedException;

@ExtendWith(MockitoExtension.class)
class TeamCommandServiceTest {

    @Mock
    private TeamQueryService teamQueryService;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamCommandService teamCommandService;

    private Team team(Long id, Status status) {
        return Team.builder().id(id).sectionId(10L).name(id + "팀").status(status).build();
    }

    @Test
    @DisplayName("분반의 모든 팀을 확정 상태로 저장한다")
    void finalizeTeams() {
        given(teamQueryService.getTeamsBySectionId(10L))
                .willReturn(List.of(team(1L, Status.FORMING), team(2L, Status.FORMING)));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamRepository).save(any());

        List<Team> finalized = teamCommandService.finalizeTeams(10L);

        assertThat(finalized).hasSize(2)
                .allSatisfy(team -> assertThat(team.getStatus()).isEqualTo(Status.CONFIRMED));
        verify(teamRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("이미 확정된 팀이 섞여 있어도 다시 확정할 수 있다")
    void finalizeTeamsIsIdempotent() {
        given(teamQueryService.getTeamsBySectionId(10L))
                .willReturn(List.of(team(1L, Status.CONFIRMED), team(2L, Status.FORMING)));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamRepository).save(any());

        assertThat(teamCommandService.finalizeTeams(10L))
                .extracting(Team::getStatus)
                .containsExactly(Status.CONFIRMED, Status.CONFIRMED);
    }

    @Test
    @DisplayName("없는 분반은 확정할 수 없다")
    void rejectsMissingSection() {
        given(teamQueryService.getTeamsBySectionId(99L)).willThrow(new SectionNotFoundException());

        assertThatThrownBy(() -> teamCommandService.finalizeTeams(99L))
                .isInstanceOf(SectionNotFoundException.class);

        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("킥오프 정보를 통째로 덮어쓴다")
    void updateKickoff() {
        Team team = team(1L, Status.FORMING);
        given(teamQueryService.getTeamById(1L)).willReturn(team);
        given(teamRepository.findAllBySectionId(10L)).willReturn(List.of(team));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamRepository).save(any());

        Team updated = teamCommandService.updateKickoff(
                1L, "새 팀명", "AI 학습 도우미", "매주 화요일 회고", "매주 목 19:00 온라인");

        assertThat(updated.getName()).isEqualTo("새 팀명");
        assertThat(updated.getTopic()).isEqualTo("AI 학습 도우미");
        assertThat(updated.getKickoffRule()).isEqualTo("매주 화요일 회고");
        assertThat(updated.getMeetingSchedule()).isEqualTo("매주 목 19:00 온라인");
    }

    @Test
    @DisplayName("확정된 팀의 킥오프 정보는 수정할 수 없다")
    void rejectsKickoffUpdateOnConfirmedTeam() {
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.CONFIRMED));

        assertThatThrownBy(() -> teamCommandService.updateKickoff(1L, "새 팀명", null, null, null))
                .isInstanceOf(TeamAlreadyConfirmedException.class);

        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 분반에 이미 있는 팀명으로는 바꿀 수 없다")
    void rejectsDuplicateName() {
        Team team = team(1L, Status.FORMING);
        given(teamQueryService.getTeamById(1L)).willReturn(team);
        given(teamRepository.findAllBySectionId(10L))
                .willReturn(List.of(team, team(2L, Status.FORMING)));

        assertThatThrownBy(() -> teamCommandService.updateKickoff(1L, "2팀", null, "k", "m"))
                .isInstanceOf(DuplicateTeamNameException.class);

        assertThat(team.getName()).isEqualTo("1팀");
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("자기 팀명을 그대로 두는 것은 중복이 아니다")
    void keepingOwnNameIsNotDuplicate() {
        Team team = team(1L, Status.FORMING);
        given(teamQueryService.getTeamById(1L)).willReturn(team);
        given(teamRepository.findAllBySectionId(10L))
                .willReturn(List.of(team, team(2L, Status.FORMING)));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamRepository).save(any());

        assertThat(teamCommandService.updateKickoff(1L, "1팀", "주제", "k", "m").getName())
                .isEqualTo("1팀");
    }
}
