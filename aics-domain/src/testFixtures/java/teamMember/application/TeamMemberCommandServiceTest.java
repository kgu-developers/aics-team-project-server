package teamMember.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.exception.TeamAlreadyConfirmedException;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;

@ExtendWith(MockitoExtension.class)
class TeamMemberCommandServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamQueryService teamQueryService;

    @InjectMocks
    private TeamMemberCommandService teamMemberCommandService;

    private TeamMember teamMember() {
        return TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(false).projectRole("백엔드")
                .build();
    }

    private Team team(Long id, Status status) {
        return Team.builder().id(id).sectionId(10L).name(id + "팀").status(status).build();
    }

    @Test
    @DisplayName("targetTeamId가 있으면 팀을 옮긴다")
    void moveToAnotherTeam() {
        TeamMember teamMember = teamMember();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamQueryService.getTeamById(2L)).willReturn(team(2L, Status.FORMING));
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999")).willReturn(Optional.empty());
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, 2L, null, null);

        assertThat(teamMember.getTeamId()).isEqualTo(2L);
        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
    }

    @Test
    @DisplayName("옮길 팀에 이미 소속된 학생이면 예외를 던진다")
    void moveToTeamAlreadyJoined() {
        TeamMember teamMember = teamMember();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamQueryService.getTeamById(2L)).willReturn(team(2L, Status.FORMING));
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999"))
                .willReturn(Optional.of(teamMember()));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(teamMember, 2L, null, null))
                .isInstanceOf(TeamMemberAlreadyExistsException.class);

        assertThat(teamMember.getTeamId()).isEqualTo(1L);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("null인 필드는 변경하지 않는다")
    void partialUpdateKeepsNullFields() {
        TeamMember teamMember = teamMember();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, true);

        assertThat(teamMember.getTeamId()).isEqualTo(1L);
        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
        assertThat(teamMember.isLeader()).isTrue();
    }

    @Test
    @DisplayName("현재 팀과 같은 targetTeamId면 중복 조회 없이 역할만 바꾼다")
    void sameTeamIdSkipsDuplicateCheck() {
        TeamMember teamMember = teamMember();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, 1L, "프론트엔드", null);

        assertThat(teamMember.getProjectRole()).isEqualTo("프론트엔드");
        verify(teamMemberRepository, never()).findByTeamIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("팀장으로 지정하면 기존 팀장을 먼저 내린다")
    void promotingLeaderDemotesCurrentLeader() {
        TeamMember teamMember = teamMember();
        TeamMember currentLeader = TeamMember.builder()
                .id(2L).teamId(1L).userId("202611111").isLeader(true).build();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.of(currentLeader));
        given(teamMemberRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        teamMemberCommandService.updateTeamMember(teamMember, null, null, true);

        assertThat(currentLeader.isLeader()).isFalse();
        assertThat(teamMember.isLeader()).isTrue();
        verify(teamMemberRepository).save(currentLeader);
        verify(teamMemberRepository).save(teamMember);
    }

    @Test
    @DisplayName("옮겨간 팀의 기존 팀장을 내린다")
    void promotingLeaderAfterMoveDemotesTargetTeamLeader() {
        TeamMember teamMember = teamMember();
        TeamMember targetTeamLeader = TeamMember.builder()
                .id(2L).teamId(2L).userId("202611111").isLeader(true).build();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamQueryService.getTeamById(2L)).willReturn(team(2L, Status.FORMING));
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999")).willReturn(Optional.empty());
        given(teamMemberRepository.findLeaderByTeamId(2L)).willReturn(Optional.of(targetTeamLeader));
        given(teamMemberRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        teamMemberCommandService.updateTeamMember(teamMember, 2L, null, true);

        assertThat(targetTeamLeader.isLeader()).isFalse();
        verify(teamMemberRepository, never()).findLeaderByTeamId(1L);
    }

    @Test
    @DisplayName("이미 팀장인 팀원을 다시 팀장으로 지정해도 자기 자신을 내리지 않는다")
    void promotingSameLeaderKeepsLeadership() {
        TeamMember teamMember = TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(true).build();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.of(teamMember));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, true);

        assertThat(teamMember.isLeader()).isTrue();
        verify(teamMemberRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("팀장에서 내릴 때는 다른 팀원을 건드리지 않는다")
    void demotingLeaderTouchesNobodyElse() {
        TeamMember teamMember = TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(true).build();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, false);

        assertThat(teamMember.isLeader()).isFalse();
        verify(teamMemberRepository, never()).findLeaderByTeamId(any());
    }

    @Test
    @DisplayName("확정된 팀의 팀원은 수정할 수 없다")
    void rejectsUpdateOnConfirmedTeam() {
        TeamMember teamMember = teamMember();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.CONFIRMED));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(teamMember, null, "프론트엔드", null))
                .isInstanceOf(TeamAlreadyConfirmedException.class);

        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("확정된 팀으로는 팀원을 옮길 수 없다")
    void rejectsMoveToConfirmedTeam() {
        TeamMember teamMember = teamMember();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamQueryService.getTeamById(2L)).willReturn(team(2L, Status.CONFIRMED));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(teamMember, 2L, null, null))
                .isInstanceOf(TeamAlreadyConfirmedException.class);

        assertThat(teamMember.getTeamId()).isEqualTo(1L);
        verify(teamMemberRepository, never()).save(any());
    }

    private TeamMember member(String userId, boolean isLeader, String projectRole) {
        return TeamMember.builder()
                .teamId(1L).userId(userId).isLeader(isLeader).projectRole(projectRole).build();
    }

    @Test
    @DisplayName("킥오프 저장 시 기존 팀장을 내리고 새 팀장과 역할분담을 반영한다")
    void updateKickoffRoles() {
        TeamMember oldLeader = member("202611111", true, "백엔드");
        TeamMember newLeader = member("202622222", false, "프론트엔드");
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of(oldLeader, newLeader));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamMemberRepository).save(any());

        teamMemberCommandService.updateKickoffRoles(1L, "202622222",
                Map.of("202611111", "기획", "202622222", "백엔드"));

        assertThat(oldLeader.isLeader()).isFalse();
        assertThat(oldLeader.getProjectRole()).isEqualTo("기획");
        assertThat(newLeader.isLeader()).isTrue();
        assertThat(newLeader.getProjectRole()).isEqualTo("백엔드");
    }

    @Test
    @DisplayName("요청에 없는 팀원의 역할은 유지하고 저장하지도 않는다")
    void keepsRolesOfUnlistedMembers() {
        TeamMember leader = member("202611111", true, "백엔드");
        TeamMember other = member("202622222", false, "프론트엔드");
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of(leader, other));

        teamMemberCommandService.updateKickoffRoles(1L, "202611111", Map.of());

        assertThat(other.getProjectRole()).isEqualTo("프론트엔드");
        assertThat(other.isLeader()).isFalse();
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("값이 바뀐 팀원만 저장한다")
    void savesOnlyChangedMembers() {
        TeamMember leader = member("202611111", true, "백엔드");
        TeamMember unchanged = member("202622222", false, "프론트엔드");
        TeamMember changed = member("202633333", false, "기획");
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findAllByTeamId(1L))
                .willReturn(List.of(leader, unchanged, changed));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamMemberRepository).save(any());

        teamMemberCommandService.updateKickoffRoles(1L, "202611111", Map.of(
                "202611111", "백엔드",        // 그대로
                "202622222", "프론트엔드",    // 그대로
                "202633333", "디자인"));      // 변경

        verify(teamMemberRepository, times(1)).save(any());
        verify(teamMemberRepository).save(changed);
    }

    @Test
    @DisplayName("팀장이 교체돼도 팀원당 저장은 한 번뿐이다")
    void savesEachMemberOnce() {
        TeamMember oldLeader = member("202611111", true, "백엔드");
        TeamMember newLeader = member("202622222", false, "프론트엔드");
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of(oldLeader, newLeader));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamMemberRepository).save(any());

        teamMemberCommandService.updateKickoffRoles(1L, "202622222", Map.of("202611111", "기획"));

        verify(teamMemberRepository, times(1)).save(oldLeader);
        verify(teamMemberRepository, times(1)).save(newLeader);
    }

    @Test
    @DisplayName("팀원이 아닌 학번을 팀장으로 지정하면 예외를 던진다")
    void rejectsLeaderOutsideTeam() {
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findAllByTeamId(1L))
                .willReturn(List.of(member("202611111", true, "백엔드")));

        assertThatThrownBy(() -> teamMemberCommandService.updateKickoffRoles(1L, "202600000", Map.of()))
                .isInstanceOf(TeamMemberNotFoundException.class);

        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("팀원이 아닌 학번에 역할을 주면 예외를 던진다")
    void rejectsRoleForOutsider() {
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findAllByTeamId(1L))
                .willReturn(List.of(member("202611111", true, "백엔드")));

        assertThatThrownBy(() -> teamMemberCommandService.updateKickoffRoles(
                1L, "202611111", Map.of("202600000", "기획")))
                .isInstanceOf(TeamMemberNotFoundException.class);

        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("확정된 팀은 킥오프 역할분담을 저장할 수 없다")
    void rejectsKickoffRolesOnConfirmedTeam() {
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.CONFIRMED));

        assertThatThrownBy(() -> teamMemberCommandService.updateKickoffRoles(1L, "202611111", Map.of()))
                .isInstanceOf(TeamAlreadyConfirmedException.class);

        verify(teamMemberRepository, never()).save(any());
    }
}
