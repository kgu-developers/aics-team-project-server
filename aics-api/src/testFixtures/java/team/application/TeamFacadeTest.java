package team.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberWithUser;
import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TeamFacadeTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private TeamCommandService teamCommandService;

  @Mock
  private TeamMemberQueryService teamMemberQueryService;

  @Mock
  private TeamMemberCommandService teamMemberCommandService;

  @Mock
  private TeamAccessValidator teamAccessValidator;

  @InjectMocks
  private TeamFacade teamFacade;

  private static final String USER = "202699999";

  private TeamMember member(Long id, String userId, boolean isLeader) {
    return TeamMember.builder().id(id).teamId(1L).userId(userId).isLeader(isLeader).build();
  }

  private TeamMemberWithUser withUser(TeamMember member, String name) {
    return new TeamMemberWithUser(member,
        User.builder().studentNumber(member.getUserId()).name(name).build());
  }

  @Test
  @DisplayName("getKickoffByTeamId는 팀 운영규칙과 회의일정을 응답한다")
  void getKickoffByTeamId() {
    given(teamQueryService.getTeamById(1L)).willReturn(Team.builder()
        .id(1L).sectionId(10L).name("1팀").kickoffRule("매주 화요일 회고")
        .meetingSchedule("매주 목 19:00").status(Status.FORMING).build());

    given(teamMemberQueryService.getTeamMembersWithUsers(1L))
        .willReturn(List.of(withUser(member(1L, "202699999", true), "김철수")));

    TeamKickoffResponse response = teamFacade.getKickoffByTeamId(1L, USER);

    assertThat(response.name()).isEqualTo("1팀");
    assertThat(response.members()).singleElement()
        .satisfies(m -> assertThat(m.isLeader()).isTrue());
    assertThat(response.kickoffRule()).isEqualTo("매주 화요일 회고");
    assertThat(response.meetingSchedule()).isEqualTo("매주 목 19:00");
  }

  @Test
  @DisplayName("updateKickoff는 팀 정보와 역할분담을 저장하고 저장 결과를 응답한다")
  void updateKickoff() {
    TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
        "1팀", "매주 화요일 회고", "매주 목 19:00", "202699999",
        List.of(new MemberRole("202699999", "백엔드")));
    given(teamCommandService.updateKickoff(1L, "1팀", "매주 화요일 회고", "매주 목 19:00"))
        .willReturn(Team.builder().id(1L).sectionId(10L).name("1팀")
            .kickoffRule("매주 화요일 회고").meetingSchedule("매주 목 19:00").status(Status.FORMING).build());
    TeamMember updated = member(1L, "202699999", true);
    given(teamMemberCommandService.updateKickoffRoles(1L, "202699999", Map.of("202699999", "백엔드")))
        .willReturn(List.of(updated));
    given(teamMemberQueryService.withUsers(List.of(updated)))
        .willReturn(List.of(withUser(updated, "김철수")));

    TeamKickoffResponse response = teamFacade.updateKickoff(1L, USER, request);

    verify(teamMemberCommandService).updateKickoffRoles(1L, "202699999", Map.of("202699999", "백엔드"));
    assertThat(response.members()).singleElement()
        .satisfies(m -> assertThat(m.name()).isEqualTo("김철수"));
  }

  @Test
  @DisplayName("updateKickoff는 역할분담이 비어도 팀장만 반영한다")
  void updateKickoffWithoutRoles() {
    TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
        "1팀", null, null, "202699999", null);
    given(teamCommandService.updateKickoff(1L, "1팀", null, null))
        .willReturn(Team.builder().id(1L).sectionId(10L).name("1팀").status(Status.FORMING).build());

    teamFacade.updateKickoff(1L, USER, request);

    verify(teamMemberCommandService).updateKickoffRoles(1L, "202699999", Map.of());
  }

  @Test
  @DisplayName("updateKickoff는 projectRole이 비어 있는 팀원이 섞여도 처리한다")
  void updateKickoffWithNullProjectRole() {
    TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
        "1팀", "매주 화요일 회고", "매주 목 19:00", "202699999",
        List.of(new MemberRole("202699999", null), new MemberRole("202611111", "백엔드")));
    given(teamCommandService.updateKickoff(1L, "1팀", "매주 화요일 회고", "매주 목 19:00"))
        .willReturn(Team.builder().id(1L).sectionId(10L).name("1팀").status(Status.FORMING).build());

    teamFacade.updateKickoff(1L, USER, request);

    Map<String, String> expected = new HashMap<>();
    expected.put("202699999", null);
    expected.put("202611111", "백엔드");
    verify(teamMemberCommandService).updateKickoffRoles(1L, "202699999", expected);
  }

  @Test
  @DisplayName("팀원도 담당 교수도 아니면 킥오프를 조회할 수 없다")
  void rejectsKickoffReadForOutsider() {
    willThrow(new AccessDeniedException("denied"))
        .given(teamAccessValidator).validateMembershipOrProfessor(1L, USER);

    assertThatThrownBy(() -> teamFacade.getKickoffByTeamId(1L, USER))
        .isInstanceOf(AccessDeniedException.class);

    verify(teamQueryService, never()).getTeamById(1L);
  }

  @Test
  @DisplayName("팀원이 아니면 킥오프를 저장할 수 없다")
  void rejectsKickoffUpdateForNonMember() {
    willThrow(new AccessDeniedException("denied"))
        .given(teamAccessValidator).validateMembership(1L, USER);
    TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
        "1팀", null, null, "202699999", null);

    assertThatThrownBy(() -> teamFacade.updateKickoff(1L, USER, request))
        .isInstanceOf(AccessDeniedException.class);

    verify(teamCommandService, never()).updateKickoff(1L, "1팀", null, null);
  }

  @Test
  @DisplayName("팀장 자진 선언은 인증된 팀원만 자신의 팀장 선언으로 처리한다")
  void claimLeader() {
    teamFacade.claimLeader(1L, USER);

    verify(teamAccessValidator).validateMembership(1L, USER);
    verify(teamMemberCommandService).claimLeader(1L, USER);
  }

  @Test
  @DisplayName("팀원이 아니면 팀장 자진 선언을 진행하지 않는다")
  void rejectsLeaderClaimForNonMember() {
    willThrow(new AccessDeniedException("denied"))
        .given(teamAccessValidator).validateMembership(1L, USER);

    assertThatThrownBy(() -> teamFacade.claimLeader(1L, USER))
        .isInstanceOf(AccessDeniedException.class);

    verify(teamMemberCommandService, never()).claimLeader(1L, USER);
  }
}
