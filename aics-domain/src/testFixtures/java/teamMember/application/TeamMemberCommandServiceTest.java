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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamAlreadyConfirmedException;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.LeaderAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.LeaderMoveRequiresExplicitRoleException;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.TeamMemberSectionMismatchException;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;

@ExtendWith(MockitoExtension.class)
class TeamMemberCommandServiceTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamQueryService teamQueryService;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamMemberCommandService teamMemberCommandService;

    private TeamMember teamMember() {
        return TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(false).projectRole("백엔드")
                .build();
    }

    private Team team(Long id, Status status) {
        return team(id, status, 10L);
    }

    private Team team(Long id, Status status, Long sectionId) {
        return Team.builder().id(id).sectionId(sectionId).name(id + "팀").status(status).build();
    }

    private TeamMember leaderOf(Long teamId, Long id) {
        return TeamMember.builder().id(id).teamId(teamId).userId("202611111").isLeader(true).build();
    }

    @Test
    @DisplayName("targetTeamId가 있으면 팀을 옮긴다")
    void moveToAnotherTeam() {
        TeamMember teamMember = teamMember();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team(2L, Status.FORMING)));
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
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team(2L, Status.FORMING)));
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
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, true);

        assertThat(teamMember.getTeamId()).isEqualTo(1L);
        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
        assertThat(teamMember.isLeader()).isTrue();
    }

    @Test
    @DisplayName("팀장이 없는 형성중 팀의 팀원은 스스로 팀장이 될 수 있다")
    void claimLeader() {
        TeamMember member = teamMember();
        Team team = team(1L, Status.FORMING);
        given(teamQueryService.getTeamById(1L)).willReturn(team);
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999")).willReturn(Optional.of(member));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.empty());
        given(teamMemberRepository.save(member)).willReturn(member);

        TeamMember claimed = teamMemberCommandService.claimLeader(1L, "202699999");

        assertThat(claimed.isLeader()).isTrue();
        assertThat(team.getStatus()).isEqualTo(Status.CONFIRMED);
        verify(teamMemberRepository).save(member);
        verify(teamRepository).save(team);
    }

    @Test
    @DisplayName("킥오프에서 이미 팀장으로 지정된 팀원은 자기 자신을 기존 팀장으로 오인하지 않고 확정할 수 있다")
    void claimLeaderAllowsSelfWhenAlreadyDesignatedLeader() {
        TeamMember member = TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(true).projectRole("백엔드")
                .build();
        Team team = team(1L, Status.FORMING);
        given(teamQueryService.getTeamById(1L)).willReturn(team);
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999")).willReturn(Optional.of(member));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.of(member));
        given(teamMemberRepository.save(member)).willReturn(member);

        TeamMember claimed = teamMemberCommandService.claimLeader(1L, "202699999");

        assertThat(claimed.isLeader()).isTrue();
        assertThat(team.getStatus()).isEqualTo(Status.CONFIRMED);
    }

    @Test
    @DisplayName("이미 팀장이 있으면 팀장 자진 선언은 409 예외를 던진다")
    void rejectsLeaderClaimWhenLeaderAlreadyExists() {
        TeamMember member = teamMember();
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999")).willReturn(Optional.of(member));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.of(leaderOf(1L, 2L)));

        assertThatThrownBy(() -> teamMemberCommandService.claimLeader(1L, "202699999"))
                .isInstanceOf(LeaderAlreadyExistsException.class);

        assertThat(member.isLeader()).isFalse();
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("확정된 팀에서는 팀장 자진 선언을 할 수 없다")
    void rejectsLeaderClaimOnConfirmedTeam() {
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.CONFIRMED));

        assertThatThrownBy(() -> teamMemberCommandService.claimLeader(1L, "202699999"))
                .isInstanceOf(TeamAlreadyConfirmedException.class);

        verify(teamMemberRepository, never()).findByTeamIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("현재 팀과 같은 targetTeamId면 중복 조회 없이 역할만 바꾼다")
    void sameTeamIdSkipsDuplicateCheck() {
        TeamMember teamMember = teamMember();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
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
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.of(currentLeader));
        given(teamMemberRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        teamMemberCommandService.updateTeamMember(teamMember, null, null, true);

        assertThat(currentLeader.isLeader()).isFalse();
        assertThat(teamMember.isLeader()).isTrue();
        verify(teamMemberRepository).save(currentLeader);
        verify(teamMemberRepository).save(teamMember);
    }

    @Test
    @DisplayName("이미 팀장인 팀원을 다시 팀장으로 지정해도 자기 자신을 내리지 않는다")
    void promotingSameLeaderKeepsLeadership() {
        TeamMember teamMember = TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(true).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, true);

        assertThat(teamMember.isLeader()).isTrue();
        verify(teamMemberRepository, times(1)).save(any());
        verify(teamMemberRepository, never()).findLeaderByTeamId(any());
    }

    @Test
    @DisplayName("팀장에서 내릴 때는 다른 팀원을 건드리지 않는다")
    void demotingLeaderTouchesNobodyElse() {
        TeamMember teamMember = TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(true).build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, false);

        assertThat(teamMember.isLeader()).isFalse();
        verify(teamMemberRepository, never()).findLeaderByTeamId(any());
    }

    @Test
    @DisplayName("확정된 팀의 팀원은 수정할 수 없다")
    void rejectsUpdateOnConfirmedTeam() {
        TeamMember teamMember = teamMember();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.CONFIRMED)));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(teamMember, null, "프론트엔드", null))
                .isInstanceOf(TeamAlreadyConfirmedException.class);

        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("확정된 팀이어도 변경할 필드가 없는 빈 PATCH는 거부하지 않는다")
    void allowsEmptyUpdateOnConfirmedTeam() {
        TeamMember teamMember = teamMember();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.CONFIRMED)));
        given(teamMemberRepository.save(teamMember)).willReturn(teamMember);

        teamMemberCommandService.updateTeamMember(teamMember, null, null, null);

        assertThat(teamMember.getProjectRole()).isEqualTo("백엔드");
        assertThat(teamMember.isLeader()).isFalse();
    }

    @Test
    @DisplayName("확정된 팀도 교수 PATCH를 통한 팀장 재배정은 허용한다")
    void allowsLeaderReassignmentOnConfirmedTeam() {
        TeamMember member = teamMember();
        TeamMember currentLeader = leaderOf(1L, 2L);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.CONFIRMED)));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.of(currentLeader));
        given(teamMemberRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        teamMemberCommandService.updateTeamMember(member, null, null, true);

        assertThat(member.isLeader()).isTrue();
        assertThat(currentLeader.isLeader()).isFalse();
        verify(teamMemberRepository).save(currentLeader);
        verify(teamMemberRepository).save(member);
    }

    @Test
    @DisplayName("확정된 팀으로는 팀원을 옮길 수 없다")
    void rejectsMoveToConfirmedTeam() {
        TeamMember teamMember = teamMember();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team(2L, Status.CONFIRMED)));

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
        teamMemberCommandService.updateKickoffRoles(1L, "202622222",
                Map.of("202611111", "기획", "202622222", "백엔드"));

        assertThat(oldLeader.isLeader()).isFalse();
        assertThat(oldLeader.getProjectRole()).isEqualTo("기획");
        assertThat(newLeader.isLeader()).isTrue();
        assertThat(newLeader.getProjectRole()).isEqualTo("백엔드");
        assertSavedInOrder(oldLeader, newLeader);
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
        verify(teamMemberRepository, never()).saveAll(any());
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
        teamMemberCommandService.updateKickoffRoles(1L, "202611111", Map.of(
                "202611111", "백엔드",        // 그대로
                "202622222", "프론트엔드",    // 그대로
                "202633333", "디자인"));      // 변경

        assertSavedInOrder(changed);
    }

    @Test
    @DisplayName("팀장이 교체돼도 팀원당 저장은 한 번뿐이다")
    void savesEachMemberOnce() {
        TeamMember oldLeader = member("202611111", true, "백엔드");
        TeamMember newLeader = member("202622222", false, "프론트엔드");
        given(teamQueryService.getTeamById(1L)).willReturn(team(1L, Status.FORMING));
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of(oldLeader, newLeader));
        teamMemberCommandService.updateKickoffRoles(1L, "202622222", Map.of("202611111", "기획"));

        assertSavedInOrder(oldLeader, newLeader);
    }

    @SuppressWarnings("unchecked")
    private void assertSavedInOrder(TeamMember... expectedMembers) {
        ArgumentCaptor<List<TeamMember>> membersCaptor = ArgumentCaptor.forClass(List.class);

        verify(teamMemberRepository).saveAll(membersCaptor.capture());

        assertThat(membersCaptor.getValue()).containsExactly(expectedMembers);
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

    @Test
    @DisplayName("다른 분반의 팀으로는 옮길 수 없다")
    void rejectsMoveToOtherSection() {
        TeamMember member = teamMember();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING, 10L)));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team(2L, Status.FORMING, 99L)));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(member, 2L, null, null))
                .isInstanceOf(TeamMemberSectionMismatchException.class);

        assertThat(member.getTeamId()).isEqualTo(1L);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("팀장이 있는 팀으로 팀장을 옮기면 409를 던지고 그 팀 팀장을 건드리지 않는다")
    void rejectsMovingLeaderIntoTeamWithLeader() {
        TeamMember member = teamMember();
        TeamMember targetLeader = leaderOf(2L, 9L);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team(2L, Status.FORMING)));
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999")).willReturn(Optional.empty());
        given(teamMemberRepository.findLeaderByTeamId(2L)).willReturn(Optional.of(targetLeader));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(member, 2L, null, true))
                .isInstanceOf(LeaderAlreadyExistsException.class);

        assertThat(targetLeader.isLeader()).isTrue();
        verify(teamMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).findLeaderByTeamId(1L);
    }

    @Test
    @DisplayName("같은 팀 안에서 팀장을 바꾸면 기존 팀장은 자동으로 내려간다")
    void demotesLeaderWithinSameTeam() {
        TeamMember member = teamMember();
        TeamMember currentLeader = leaderOf(1L, 9L);
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.of(currentLeader));
        willAnswer(invocation -> invocation.getArgument(0)).given(teamMemberRepository).save(any());

        teamMemberCommandService.updateTeamMember(member, null, null, true);

        assertThat(currentLeader.isLeader()).isFalse();
        assertThat(member.isLeader()).isTrue();
        verify(teamMemberRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("기존 팀장을 isLeader 없이 옮기면 팀장 유지 여부를 명시하라며 거절한다")
    void rejectsMovingExistingLeaderWithoutExplicitIsLeader() {
        TeamMember existingLeader = TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(true).projectRole("백엔드")
                .build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));

        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(existingLeader, 2L, null, null))
                .isInstanceOf(LeaderMoveRequiresExplicitRoleException.class);

        assertThat(existingLeader.getTeamId()).isEqualTo(1L);
        assertThat(existingLeader.isLeader()).isTrue();
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("isLeader=false로 옮기면 팀장직을 내려놓고 대상 팀 팀장 자리를 넘보지 않는다")
    void movingExistingLeaderWithIsLeaderFalseDemotesAndMoves() {
        TeamMember existingLeader = TeamMember.builder()
                .id(1L).teamId(1L).userId("202699999").isLeader(true).projectRole("백엔드")
                .build();
        given(teamRepository.findById(1L)).willReturn(Optional.of(team(1L, Status.FORMING)));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team(2L, Status.FORMING)));
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999")).willReturn(Optional.empty());
        given(teamMemberRepository.save(existingLeader)).willReturn(existingLeader);

        teamMemberCommandService.updateTeamMember(existingLeader, 2L, null, false);

        assertThat(existingLeader.getTeamId()).isEqualTo(2L);
        assertThat(existingLeader.isLeader()).isFalse();
        verify(teamMemberRepository, never()).findLeaderByTeamId(any());
    }

    @Test
    @DisplayName("팀 이동 시 버전 충돌을 감지한다")
    void detectVersionConflictOnTeamMove() {
        TeamMember teamMember = teamMember();
        Team team1v1 = Team.builder().id(1L).sectionId(10L).name("1팀").status(Status.FORMING).version(1L).build();
        Team team2v1 = Team.builder().id(2L).sectionId(10L).name("2팀").status(Status.FORMING).version(1L).build();
        
        given(teamRepository.findById(1L)).willReturn(Optional.of(team1v1));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team2v1));
        given(teamMemberRepository.findByTeamIdAndUserId(2L, "202699999")).willReturn(Optional.empty());
        
        teamMemberCommandService.updateTeamMember(teamMember, 2L, null, null);
        
        Team team1v2 = Team.builder().id(1L).sectionId(10L).name("1팀").status(Status.FORMING).version(2L).build();
        Team team2v2 = Team.builder().id(2L).sectionId(10L).name("2팀").status(Status.FORMING).version(2L).build();
        
        given(teamRepository.findById(1L)).willReturn(Optional.of(team1v2));
        given(teamRepository.findById(2L)).willReturn(Optional.of(team2v2));
        
        teamMemberCommandService.updateTeamMember(teamMember, 1L, null, null);
        
        assertThat(teamMember.getTeamId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("팀 확정과 팀원 수정 동시 실행 시 경쟁 상태를 방지한다")
    void preventsRaceConditionBetweenTeamConfirmationAndMemberUpdate() {
        TeamMember member = teamMember();
        Team confirmedTeam = team(1L, Status.CONFIRMED);
        
        given(teamRepository.findById(1L)).willReturn(Optional.of(confirmedTeam));
        
        assertThatThrownBy(() -> teamMemberCommandService.updateTeamMember(member, null, "프론트엔드", null))
                .isInstanceOf(TeamAlreadyConfirmedException.class);
        
        assertThat(member.getProjectRole()).isEqualTo("백엔드");
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("동시에 팀 확정과 팀원 수정 요청이 들어올 때 하나만 성공한다")
    void concurrentTeamConfirmationAndMemberUpdate() throws InterruptedException {
        TeamMember member = teamMember();
        Team formingTeam = team(1L, Status.FORMING);
        Team confirmedTeam = team(1L, Status.CONFIRMED);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        
        given(teamQueryService.getTeamById(1L)).willReturn(formingTeam);
        given(teamMemberRepository.findByTeamIdAndUserId(1L, "202699999")).willReturn(Optional.of(member));
        given(teamMemberRepository.findLeaderByTeamId(1L)).willReturn(Optional.empty());
        given(teamMemberRepository.save(member)).willReturn(member);
        given(teamRepository.save(formingTeam)).willReturn(formingTeam);
        
        executor.submit(() -> {
            try {
                teamMemberCommandService.claimLeader(1L, "202699999");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
        
        executor.submit(() -> {
            try {
                Thread.sleep(10);
                given(teamRepository.findById(1L)).willReturn(Optional.of(confirmedTeam));
                teamMemberCommandService.updateTeamMember(member, null, "프론트엔드", null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
        
        latch.await();
        executor.shutdown();
        
        assertThat(successCount.get() + failureCount.get()).isEqualTo(2);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }
}
