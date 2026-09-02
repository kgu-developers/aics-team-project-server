package teamMember.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.teamMember.application.TeamMemberAdminFacade;
import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import java.util.List;

import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TeamMemberAdminFacadeTest {

  @Mock
  private TeamMemberQueryService teamMemberQueryService;

  @Mock
  private TeamMemberCommandService teamMemberCommandService;

  @Mock
  private UserQueryService userQueryService;

  @InjectMocks
  private TeamMemberAdminFacade teamMemberAdminFacade;

  private TeamMember member() {
    return TeamMember.builder()
        .id(1L).teamId(1L).userId("202699999").isLeader(false).projectRole("백엔드").build();
  }

  @Test
  @DisplayName("수정 결과에 사용자 이름을 채워 응답한다")
  void updateTeamMemberWithTargetTeam() {
    TeamMember found = member();
    TeamMember updated = TeamMember.builder()
        .id(1L).teamId(2L).userId("202699999").isLeader(true).projectRole("프론트엔드").build();
    TeamMemberUpdateRequest request = new TeamMemberUpdateRequest(2L, "프론트엔드", true);

    given(teamMemberQueryService.getTeamMember(1L, "202699999")).willReturn(found);
    given(teamMemberCommandService.updateTeamMember(found, 2L, "프론트엔드", true)).willReturn(updated);
    given(userQueryService.getUsersByStudentNumbers(List.of("202699999"))).willReturn(
        List.of(User.builder().studentNumber("202699999").name("김철수").build()));

    TeamMemberAdminResponse response = teamMemberAdminFacade.updateTeamMember(1L, "202699999", request);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.studentNumber()).isEqualTo("202699999");
    assertThat(response.name()).isEqualTo("김철수");
    assertThat(response.isLeader()).isTrue();
    assertThat(response.projectRole()).isEqualTo("프론트엔드");
  }

  @Test
  @DisplayName("요청 값을 그대로 도메인 서비스에 넘긴다")
  void updateTeamMemberWithoutTargetTeam() {
    TeamMember found = member();
    TeamMemberUpdateRequest request = new TeamMemberUpdateRequest(null, "프론트엔드", null);

    given(teamMemberQueryService.getTeamMember(1L, "202699999")).willReturn(found);
    given(teamMemberCommandService.updateTeamMember(found, null, "프론트엔드", null)).willReturn(found);
    given(userQueryService.getUsersByStudentNumbers(List.of("202699999"))).willReturn(
        List.of(User.builder().studentNumber("202699999").name("김철수").build()));

    teamMemberAdminFacade.updateTeamMember(1L, "202699999", request);

    verify(teamMemberCommandService).updateTeamMember(found, null, "프론트엔드", null);
  }

  @Test
  @DisplayName("사용자가 소프트 삭제돼도 수정은 롤백되지 않고 이름만 비운 채 응답한다")
  void keepsUpdateWhenUserMissing() {
    TeamMemberUpdateRequest request = new TeamMemberUpdateRequest(null, "프론트엔드", null);
    TeamMember found = member();
    given(teamMemberQueryService.getTeamMember(1L, "202699999")).willReturn(found);
    given(teamMemberCommandService.updateTeamMember(found, null, "프론트엔드", null)).willReturn(found);
    given(userQueryService.getUsersByStudentNumbers(List.of("202699999"))).willReturn(List.of());

    TeamMemberAdminResponse response = teamMemberAdminFacade.updateTeamMember(1L, "202699999", request);

    assertThat(response.studentNumber()).isEqualTo("202699999");
    assertThat(response.name()).isNull();
  }
}
