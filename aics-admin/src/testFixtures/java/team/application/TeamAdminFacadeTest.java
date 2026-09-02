package team.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.team.application.TeamAdminFacade;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMemberWithUser;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TeamAdminFacadeTest {

	@Mock
	private TeamQueryService teamQueryService;

	@Mock
	private TeamCommandService teamCommandService;

	@Mock
	private TeamMemberQueryService teamMemberQueryService;

	@InjectMocks
	private TeamAdminFacade teamAdminFacade;

	private final Team team = Team.builder()
		.id(1L).sectionId(10L).name("1팀").status(Status.FORMING).build();

	private TeamMember member(Long id, String userId, boolean isLeader) {
		return TeamMember.builder().id(id).teamId(1L).userId(userId).isLeader(isLeader).build();
	}

	private TeamMemberWithUser withUser(TeamMember member, String name) {
		return new TeamMemberWithUser(member,
			User.builder().studentNumber(member.getUserId()).name(name).build());
	}

	@Test
	@DisplayName("getTeamById는 팀원 목록에 사용자 이름을 채워 응답한다")
	void getTeamById() {
		given(teamQueryService.getTeamById(1L)).willReturn(team);
		given(teamMemberQueryService.getTeamMembersWithUsers(1L)).willReturn(List.of(
			withUser(member(1L, "202699999", true), "김철수"),
			withUser(member(2L, "202611111", false), "이영희")));

		TeamAdminDetailResponse response = teamAdminFacade.getTeamById(1L);

		assertThat(response.sectionId()).isEqualTo(10L);
		assertThat(response.members()).extracting("studentNumber", "name", "isLeader")
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple("202699999", "김철수", true),
				org.assertj.core.groups.Tuple.tuple("202611111", "이영희", false));
	}

	@Test
	@DisplayName("getTeamById는 사용자가 조회되지 않아도 이름만 비운 채 응답한다")
	void getTeamByIdWithMissingUser() {
		given(teamQueryService.getTeamById(1L)).willReturn(team);
		given(teamMemberQueryService.getTeamMembersWithUsers(1L))
			.willReturn(List.of(new TeamMemberWithUser(member(1L, "202699999", true), null)));

		TeamAdminDetailResponse response = teamAdminFacade.getTeamById(1L);

		assertThat(response.members()).singleElement()
			.satisfies(m -> {
				assertThat(m.studentNumber()).isEqualTo("202699999");
				assertThat(m.name()).isNull();
			});
	}

	@Test
	@DisplayName("finalizeTeams는 확정된 팀 목록을 응답한다")
	void finalizeTeams() {
		given(teamCommandService.finalizeTeams(10L)).willReturn(List.of(
			Team.builder().id(1L).sectionId(10L).name("1팀").status(Status.CONFIRMED).build(),
			Team.builder().id(2L).sectionId(10L).name("2팀").status(Status.CONFIRMED).build()));

		TeamAdminListResponse response = teamAdminFacade.finalizeTeams(10L);

		assertThat(response.contents()).extracting("name", "status")
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple("1팀", Status.CONFIRMED),
				org.assertj.core.groups.Tuple.tuple("2팀", Status.CONFIRMED));
	}
}
