package team.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import kgu.developers.admin.team.application.TeamAdminFacade;
import kgu.developers.admin.team.presentation.TeamAdminControllerImpl;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.domain.team.domain.Status;

@ExtendWith(MockitoExtension.class)
class TeamAdminControllerTest {

	private static final String REASON = "팀 배정 오류를 수정하고 학생들의 팀 구성을 다시 조정하기 위해 취소합니다.";

	private static final String BASE_URL = "/api/v1/admin";

	@Mock
	private TeamAdminFacade teamAdminFacade;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TeamAdminControllerImpl(teamAdminFacade)).build();
	}

	private TeamAdminDetailResponse response() {
		return new TeamAdminDetailResponse(
			1L, 10L, "1팀", "매주 화요일 회고", "매주 목 19:00", Status.FORMING,
			List.of(
				new TeamMemberAdminResponse(1L, "202699999", "김철수", "컴퓨터공학과", true, "백엔드"),
				new TeamMemberAdminResponse(2L, "202611111", "이영희", "산업경영공학과", false, "프론트엔드")),
			LocalDateTime.of(2026, 3, 2, 10, 0));
	}

	@Test
	@DisplayName("팀 상세를 조회하면 200과 팀원 목록을 응답한다")
	void getTeamById() throws Exception {
		given(teamAdminFacade.getTeamById(1L)).willReturn(response());

		mockMvc.perform(get(BASE_URL + "/teams/{teamId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.sectionId").value(10))
			.andExpect(jsonPath("$.name").value("1팀"))
			.andExpect(jsonPath("$.status").value("FORMING"))
			.andExpect(jsonPath("$.members.length()").value(2))
			.andExpect(jsonPath("$.members[0].studentNumber").value("202699999"))
			.andExpect(jsonPath("$.members[0].major").value("컴퓨터공학과"))
			.andExpect(jsonPath("$.members[0].isLeader").value(true))
			.andExpect(jsonPath("$.members[1].projectRole").value("프론트엔드"));

		verify(teamAdminFacade).getTeamById(1L);
	}

	@Test
	@DisplayName("팀 배정을 확정하면 200과 확정된 팀 목록을 응답한다")
	void finalizeTeams() throws Exception {
		given(teamAdminFacade.finalizeTeams(10L, "admin")).willReturn(new TeamAdminListResponse(List.of(
			new TeamAdminResponse(1L, "1팀", null, null, Status.CONFIRMED, null),
			new TeamAdminResponse(2L, "2팀", null, null, Status.CONFIRMED, null))));

		mockMvc.perform(patch(BASE_URL + "/sections/{sectionId}/teams/finalize", 10L)
			.principal(new UsernamePasswordAuthenticationToken("admin", null)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contents.length()").value(2))
			.andExpect(jsonPath("$.contents[0].name").value("1팀"))
			.andExpect(jsonPath("$.contents[0].status").value("CONFIRMED"))
			.andExpect(jsonPath("$.contents[1].status").value("CONFIRMED"));

		verify(teamAdminFacade).finalizeTeams(10L, "admin");
	}

	@Test
	@DisplayName("확정 취소는 요청 본문이 없으면 거부한다")
	void unfinalizeRejectsMissingRequestBody() throws Exception {
		mockMvc.perform(patch(BASE_URL + "/sections/{sectionId}/teams/unfinalize", 10L)
			.principal(new UsernamePasswordAuthenticationToken("admin", null)))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(teamAdminFacade);
	}

	@Test
	@DisplayName("취소 사유를 전달하여 분반 팀 배정 확정을 취소한다")
	void unfinalizeTeamsWithReason() throws Exception {
		given(teamAdminFacade.unfinalizeTeams(10L, "admin", REASON)).willReturn(new TeamAdminListResponse(List.of(
			new TeamAdminResponse(1L, "1팀", null, null, Status.FORMING, null))));
		mockMvc.perform(patch(BASE_URL + "/sections/{sectionId}/teams/unfinalize", 10L)
			.contentType("application/json").content("{\"reason\":\"  " + REASON + "  \"}")
			.principal(new UsernamePasswordAuthenticationToken("admin", null)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contents[0].status").value("FORMING"));
		verify(teamAdminFacade).unfinalizeTeams(10L, "admin", REASON);
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"reason\":null}", "{\"reason\":\"\"}", "{\"reason\":\"   \"}"})
	@DisplayName("확정 취소는 사유 누락과 공백을 거부한다")
	void unfinalizeRejectsInvalidReason(String body) throws Exception {
		mockMvc.perform(patch(BASE_URL + "/sections/{sectionId}/teams/unfinalize", 10L)
			.contentType("application/json").content(body)
			.principal(new UsernamePasswordAuthenticationToken("admin", null)))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(teamAdminFacade);
	}

	@ParameterizedTest
	@ValueSource(ints = {29, 501})
	@DisplayName("확정 취소는 공백 제거 후 30~500자 범위를 벗어난 사유를 거부한다")
	void unfinalizeRejectsReasonLength(int length) throws Exception {
		mockMvc.perform(patch(BASE_URL + "/sections/{sectionId}/teams/unfinalize", 10L)
			.contentType("application/json").content("{\"reason\":\"  " + "가".repeat(length) + "  \"}")
			.principal(new UsernamePasswordAuthenticationToken("admin", null)))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(teamAdminFacade);
	}

}
