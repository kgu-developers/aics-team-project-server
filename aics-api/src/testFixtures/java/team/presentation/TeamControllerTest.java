package team.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.api.team.presentation.TeamControllerImpl;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest.MemberRole;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

	private static final String BASE_URL = "/api/v1";
	private static final String STUDENT_NUMBER = "202699999";

	@Mock
	private TeamFacade teamFacade;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Authentication authentication =
		new UsernamePasswordAuthenticationToken(STUDENT_NUMBER, null);

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TeamControllerImpl(teamFacade)).build();
	}

	private TeamKickoffResponse response() {
		return new TeamKickoffResponse(1L, "1팀", "매주 화요일 회고", "매주 목 19:00",
			List.of(new TeamMemberResponse(1L, "202699999", "김철수", true, "백엔드")));
	}

	@Test
	@DisplayName("킥오프 정보를 조회하면 200과 운영규칙, 회의일정을 응답한다")
	void getKickoffByTeamId() throws Exception {
		given(teamFacade.getKickoffByTeamId(1L, STUDENT_NUMBER)).willReturn(response());

		mockMvc.perform(get(BASE_URL + "/teams/{teamId}/kickoff", 1L).principal(authentication))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.name").value("1팀"))
			.andExpect(jsonPath("$.kickoffRule").value("매주 화요일 회고"))
			.andExpect(jsonPath("$.meetingSchedule").value("매주 목 19:00"))
			.andExpect(jsonPath("$.members[0].projectRole").value("백엔드"));

		verify(teamFacade).getKickoffByTeamId(1L, STUDENT_NUMBER);
	}

	@Test
	@DisplayName("킥오프 정보를 저장하면 200과 저장된 정보를 응답한다")
	void updateKickoff() throws Exception {
		TeamKickoffUpdateRequest request = new TeamKickoffUpdateRequest(
			"1팀", "매주 화요일 회고", "매주 목 19:00", "202699999",
			List.of(new MemberRole("202699999", "백엔드")));
		given(teamFacade.updateKickoff(eq(1L), eq(STUDENT_NUMBER), any())).willReturn(response());

		mockMvc.perform(put(BASE_URL + "/teams/{teamId}/kickoff", 1L)
				.principal(authentication)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.members[0].isLeader").value(true))
			.andExpect(jsonPath("$.members[0].projectRole").value("백엔드"));

		verify(teamFacade).updateKickoff(eq(1L), eq(STUDENT_NUMBER), any());
	}

	@Test
	@DisplayName("팀명이나 팀장 학번이 비면 400을 응답한다")
	void rejectsBlankRequiredFields() throws Exception {
		mockMvc.perform(put(BASE_URL + "/teams/{teamId}/kickoff", 1L)
				.principal(authentication)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\",\"leaderStudentNumber\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("팀장이 없으면 팀원이 자진 선언해 204를 응답한다")
	void claimLeader() throws Exception {
		mockMvc.perform(post(BASE_URL + "/teams/{teamId}/leader-claim", 1L)
				.principal(authentication))
			.andExpect(status().isNoContent());

		verify(teamFacade).claimLeader(1L, STUDENT_NUMBER);
	}
}
