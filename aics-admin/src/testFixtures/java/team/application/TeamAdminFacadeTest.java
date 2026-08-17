package team.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.admin.team.application.TeamAdminFacade;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.admin.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.admin.team.presentation.response.TeamAdminKickoffResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.domain.team.application.command.TeamCommandService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.application.command.TeamMemberCommandService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TeamAdminFacadeTest {

	@Mock
	private TeamQueryService teamQueryService;

	@Mock
	private TeamCommandService teamCommandService;

	@Mock
	private TeamMemberQueryService teamMemberQueryService;

	@Mock
	private TeamMemberCommandService teamMemberCommandService;

	@Mock
	private UserQueryService userQueryService;

	@InjectMocks
	private TeamAdminFacade teamAdminFacade;

	private final Team team = Team.builder()
		.id(1L).sectionId(10L).name("1팀").status(Status.FORMING).build();

	private TeamMember member(Long id, String userId, boolean isLeader) {
		return TeamMember.builder().id(id).teamId(1L).userId(userId).isLeader(isLeader).build();
	}

	@Test
	@DisplayName("getTeamById는 팀원 목록에 사용자 이름을 채워 응답한다")
	void getTeamById() {
		given(teamQueryService.getTeamById(1L)).willReturn(team);
		given(teamMemberQueryService.getTeamMembersByTeamId(1L))
			.willReturn(List.of(member(1L, "202699999", true), member(2L, "202611111", false)));
		given(userQueryService.getUsersByStudentNumbers(List.of("202699999", "202611111")))
			.willReturn(List.of(
				User.builder().studentNumber("202699999").name("김철수").build(),
				User.builder().studentNumber("202611111").name("이영희").build()));

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
		given(teamMemberQueryService.getTeamMembersByTeamId(1L))
			.willReturn(List.of(member(1L, "202699999", true)));
		given(userQueryService.getUsersByStudentNumbers(List.of("202699999")))
			.willReturn(List.of());

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

	@Test
	@DisplayName("getKickoffByTeamId는 팀 운영규칙과 회의일정을 응답한다")
	void getKickoffByTeamId() {
		given(teamQueryService.getTeamById(1L)).willReturn(Team.builder()
			.id(1L).sectionId(10L).name("1팀").topic("AI 학습 도우미").kickoffRule("매주 화요일 회고")
			.meetingSchedule("매주 목 19:00").status(Status.FORMING).build());

		given(teamMemberQueryService.getTeamMembersByTeamId(1L))
			.willReturn(List.of(member(1L, "202699999", true)));
		given(userQueryService.getUsersByStudentNumbers(List.of("202699999")))
			.willReturn(List.of(User.builder().studentNumber("202699999").name("김철수").build()));

		TeamAdminKickoffResponse response = teamAdminFacade.getKickoffByTeamId(1L);

		assertThat(response.name()).isEqualTo("1팀");
		assertThat(response.members()).singleElement()
			.satisfies(m -> assertThat(m.isLeader()).isTrue());
		assertThat(response.topic()).isEqualTo("AI 학습 도우미");
		assertThat(response.kickoffRule()).isEqualTo("매주 화요일 회고");
		assertThat(response.meetingSchedule()).isEqualTo("매주 목 19:00");
	}

	@Test
	@DisplayName("updateKickoff는 팀 정보와 역할분담을 저장하고 저장 결과를 응답한다")
	void updateKickoff() {
		TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
			"1팀", "AI 학습 도우미", "매주 화요일 회고", "매주 목 19:00", "202699999",
			List.of(new MemberRole("202699999", "백엔드")));
		given(teamCommandService.updateKickoff(1L, "1팀", "AI 학습 도우미", "매주 화요일 회고", "매주 목 19:00"))
			.willReturn(Team.builder().id(1L).sectionId(10L).name("1팀").topic("AI 학습 도우미")
				.kickoffRule("매주 화요일 회고").meetingSchedule("매주 목 19:00").status(Status.FORMING).build());
		given(teamMemberQueryService.getTeamMembersByTeamId(1L))
			.willReturn(List.of(member(1L, "202699999", true)));
		given(userQueryService.getUsersByStudentNumbers(List.of("202699999")))
			.willReturn(List.of(User.builder().studentNumber("202699999").name("김철수").build()));

		TeamAdminKickoffResponse response = teamAdminFacade.updateKickoff(1L, request);

		verify(teamMemberCommandService).updateKickoffRoles(1L, "202699999", Map.of("202699999", "백엔드"));
		assertThat(response.topic()).isEqualTo("AI 학습 도우미");
		assertThat(response.members()).singleElement()
			.satisfies(m -> assertThat(m.name()).isEqualTo("김철수"));
	}
}
