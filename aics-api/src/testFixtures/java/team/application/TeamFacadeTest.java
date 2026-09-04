package team.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import kgu.developers.domain.auditLog.application.command.AuditLogCommandService;
import kgu.developers.domain.auditLog.domain.AuditLogEventType;
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

  @Mock
  private AuditLogCommandService auditLogCommandService;

  @InjectMocks
  private TeamFacade teamFacade;

  private static final String USER = "202699999";

  private TeamMember member(Long id, String userId, boolean isLeader) {
    return TeamMember.builder().id(id).teamId(1L).userId(userId).isLeader(isLeader).build();
  }

  private Team team(String name, String kickoffRule, String meetingSchedule) {
    return Team.builder().id(1L).sectionId(10L).name(name).kickoffRule(kickoffRule)
        .meetingSchedule(meetingSchedule).status(Status.FORMING).build();
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
    given(teamQueryService.getTeamByIdForUpdate(1L))
        .willReturn(team("기존 팀", "기존 규칙", "기존 일정"));
    given(teamMemberQueryService.getTeamMembersByTeamId(1L))
        .willReturn(List.of(member(1L, "202699999", false)));
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
    given(teamQueryService.getTeamByIdForUpdate(1L)).willReturn(team("1팀", null, null));

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
    given(teamQueryService.getTeamByIdForUpdate(1L))
        .willReturn(team("1팀", "매주 화요일 회고", "매주 목 19:00"));

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
    verify(auditLogCommandService, never()).recordTeamChange(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("팀장 자진 선언은 인증된 팀원만 자신의 팀장 선언으로 처리한다")
  void claimLeader() {
    Team team = team("1팀", "규칙", "일정");
    TeamMember before = member(1L, USER, false);
    TeamMember after = member(1L, USER, true);
    given(teamQueryService.getTeamByIdForUpdate(1L)).willReturn(team);
    given(teamMemberQueryService.getTeamMembersByTeamId(1L))
        .willReturn(List.of(before), List.of(after));
    given(teamMemberCommandService.claimLeader(team, USER)).willAnswer(invocation -> {
      team.updateStatus(Status.CONFIRMED);
      return after;
    });

    teamFacade.claimLeader(1L, USER);

    InOrder updateOrder = inOrder(
        teamQueryService, teamMemberQueryService, teamMemberCommandService);
    updateOrder.verify(teamQueryService).getTeamByIdForUpdate(1L);
    updateOrder.verify(teamMemberQueryService).getTeamMembersByTeamId(1L);
    updateOrder.verify(teamMemberCommandService).claimLeader(team, USER);
    verify(teamAccessValidator).validateMembership(1L, USER);
    verify(teamMemberCommandService).claimLeader(team, USER);
    verify(auditLogCommandService).recordTeamChange(
        org.mockito.ArgumentMatchers.eq(USER), org.mockito.ArgumentMatchers.eq(10L),
        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(AuditLogEventType.TEAM_UPDATED),
        org.mockito.ArgumentMatchers.argThat(metadata ->
            "LEADER_CLAIMED".equals(metadata.path("changeType").asText())));
    verify(auditLogCommandService).recordTeamChange(
        org.mockito.ArgumentMatchers.eq(USER), org.mockito.ArgumentMatchers.eq(10L),
        org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(AuditLogEventType.TEAM_UPDATED),
        org.mockito.ArgumentMatchers.argThat(metadata ->
            "TEAM_STATUS_UPDATED".equals(metadata.path("changeType").asText())
                && "FORMING".equals(metadata.at("/before/status").asText())
                && "CONFIRMED".equals(metadata.at("/after/status").asText())));
    verify(auditLogCommandService, times(2)).recordTeamChange(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("팀원이 아니면 팀장 자진 선언을 진행하지 않는다")
  void rejectsLeaderClaimForNonMember() {
    willThrow(new AccessDeniedException("denied"))
        .given(teamAccessValidator).validateMembership(1L, USER);

    assertThatThrownBy(() -> teamFacade.claimLeader(1L, USER))
        .isInstanceOf(AccessDeniedException.class);

    verify(teamMemberCommandService, never()).claimLeader(any(Team.class), eq(USER));
  }

  @Test
  @DisplayName("킥오프 변경은 팀명, 운영 규칙, 팀원 변경 순서로 감사 로그를 저장한다")
  void recordsKickoffChangesInOrder() {
    Team beforeTeam = team("기존 팀", "기존 규칙", "기존 일정");
    Team afterTeam = team("새 팀", "새 규칙", "새 일정");
    TeamMember beforeMember = TeamMember.builder().id(1L).teamId(1L).userId(USER)
        .isLeader(false).projectRole("백엔드").build();
    TeamMember afterMember = TeamMember.builder().id(1L).teamId(1L).userId(USER)
        .isLeader(true).projectRole("프론트엔드").build();
    TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
        "새 팀", "새 규칙", "새 일정", USER,
        List.of(new MemberRole(USER, "프론트엔드")));

    given(teamQueryService.getTeamByIdForUpdate(1L)).willReturn(beforeTeam);
    given(teamMemberQueryService.getTeamMembersByTeamId(1L)).willReturn(List.of(beforeMember));
    given(teamCommandService.updateKickoff(1L, "새 팀", "새 규칙", "새 일정"))
        .willReturn(afterTeam);
    given(teamMemberCommandService.updateKickoffRoles(1L, USER, Map.of(USER, "프론트엔드")))
        .willReturn(List.of(afterMember));

    teamFacade.updateKickoff(1L, USER, request);

    InOrder order = inOrder(auditLogCommandService);
    order.verify(auditLogCommandService).recordTeamChange(
        org.mockito.ArgumentMatchers.eq(USER), org.mockito.ArgumentMatchers.eq(10L),
        org.mockito.ArgumentMatchers.eq(1L),
        org.mockito.ArgumentMatchers.eq(AuditLogEventType.TEAM_NAME_UPDATED),
        org.mockito.ArgumentMatchers.argThat(metadata ->
            "기존 팀".equals(metadata.at("/before/name").asText())
                && "새 팀".equals(metadata.at("/after/name").asText())));
    order.verify(auditLogCommandService).recordTeamChange(
        org.mockito.ArgumentMatchers.eq(USER), org.mockito.ArgumentMatchers.eq(10L),
        org.mockito.ArgumentMatchers.eq(1L),
        org.mockito.ArgumentMatchers.eq(AuditLogEventType.TEAM_RULE_UPDATED),
        org.mockito.ArgumentMatchers.argThat(metadata ->
            "기존 규칙".equals(metadata.at("/before/kickoffRule").asText())
                && "새 일정".equals(metadata.at("/after/meetingSchedule").asText())));
    order.verify(auditLogCommandService).recordTeamChange(
        org.mockito.ArgumentMatchers.eq(USER), org.mockito.ArgumentMatchers.eq(10L),
        org.mockito.ArgumentMatchers.eq(1L),
        org.mockito.ArgumentMatchers.eq(AuditLogEventType.TEAM_UPDATED),
        org.mockito.ArgumentMatchers.argThat(metadata ->
            "KICKOFF_MEMBERS_UPDATED".equals(metadata.path("changeType").asText())
                && metadata.at("/after/members/0/leader").asBoolean()));
    verify(auditLogCommandService, times(3)).recordTeamChange(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("킥오프 값이 실제로 바뀌지 않으면 감사 로그를 저장하지 않는다")
  void skipsAuditLogForNoOpKickoffUpdate() {
    Team unchanged = team("1팀", "규칙", "일정");
    TeamMember member = TeamMember.builder().id(1L).teamId(1L).userId(USER)
        .isLeader(true).projectRole("백엔드").build();
    TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
        "1팀", "규칙", "일정", USER, List.of(new MemberRole(USER, "백엔드")));

    given(teamQueryService.getTeamByIdForUpdate(1L)).willReturn(unchanged);
    given(teamMemberQueryService.getTeamMembersByTeamId(1L)).willReturn(List.of(member));
    given(teamCommandService.updateKickoff(1L, "1팀", "규칙", "일정")).willReturn(unchanged);
    given(teamMemberCommandService.updateKickoffRoles(1L, USER, Map.of(USER, "백엔드")))
        .willReturn(List.of(member));

    teamFacade.updateKickoff(1L, USER, request);

    verify(auditLogCommandService, never()).recordTeamChange(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("킥오프 변경 중 팀원 저장이 실패하면 감사 로그를 만들지 않는다")
  void skipsAuditLogWhenKickoffUpdateFails() {
    Team before = team("기존 팀", "기존 규칙", "기존 일정");
    Team after = team("새 팀", "새 규칙", "새 일정");
    TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
        "새 팀", "새 규칙", "새 일정", USER, List.of());

    given(teamQueryService.getTeamByIdForUpdate(1L)).willReturn(before);
    given(teamCommandService.updateKickoff(1L, "새 팀", "새 규칙", "새 일정"))
        .willReturn(after);
    given(teamMemberCommandService.updateKickoffRoles(1L, USER, Map.of()))
        .willThrow(new IllegalStateException("팀원 저장 실패"));

    assertThatThrownBy(() -> teamFacade.updateKickoff(1L, USER, request))
        .isInstanceOf(IllegalStateException.class);

    verify(auditLogCommandService, never()).recordTeamChange(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }
}
