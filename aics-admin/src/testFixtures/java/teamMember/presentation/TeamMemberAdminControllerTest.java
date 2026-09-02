package teamMember.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import kgu.developers.admin.teamMember.application.TeamMemberAdminFacade;
import kgu.developers.admin.teamMember.presentation.TeamMemberAdminControllerImpl;
import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;

@ExtendWith(MockitoExtension.class)
class TeamMemberAdminControllerTest {

	private static final String BASE_URL = "/api/v1/admin/oop/teams/{teamId}/members/{studentNumber}";
	private static final String STUDENT_NUMBER = "202699999";
	private static final String ADMIN_ID = "admin001";

	@Mock
	private TeamMemberAdminFacade teamMemberAdminFacade;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Authentication authentication =
		new UsernamePasswordAuthenticationToken(ADMIN_ID, null);

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new TeamMemberAdminControllerImpl(teamMemberAdminFacade))
			.build();
	}

	@Test
	@DisplayName("팀원을 수정하면 200과 수정된 팀원을 응답한다")
	void updateTeamMember() throws Exception {
		TeamMemberUpdateRequest request = new TeamMemberUpdateRequest(2L, "프론트엔드", true);
		given(teamMemberAdminFacade.updateTeamMember(1L, STUDENT_NUMBER, request, ADMIN_ID))
			.willReturn(new TeamMemberAdminResponse(1L, STUDENT_NUMBER, "김철수", true, "프론트엔드"));

		mockMvc.perform(patch(BASE_URL, 1L, STUDENT_NUMBER)
				.principal(authentication)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.studentNumber").value(STUDENT_NUMBER))
			.andExpect(jsonPath("$.name").value("김철수"))
			.andExpect(jsonPath("$.isLeader").value(true))
			.andExpect(jsonPath("$.projectRole").value("프론트엔드"));

		verify(teamMemberAdminFacade).updateTeamMember(1L, STUDENT_NUMBER, request, ADMIN_ID);
	}

	@Test
	@DisplayName("빈 요청 본문이면 아무 필드도 바꾸지 않고 200을 응답한다")
	void updateTeamMemberWithEmptyBody() throws Exception {
		TeamMemberUpdateRequest empty = new TeamMemberUpdateRequest(null, null, null);
		given(teamMemberAdminFacade.updateTeamMember(1L, STUDENT_NUMBER, empty, ADMIN_ID))
			.willReturn(new TeamMemberAdminResponse(1L, STUDENT_NUMBER, "김철수", false, "백엔드"));

		mockMvc.perform(patch(BASE_URL, 1L, STUDENT_NUMBER)
				.principal(authentication)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.projectRole").value("백엔드"));
	}

	@Test
	@DisplayName("targetTeamId가 양수가 아니면 400을 응답한다")
	void rejectsNonPositiveTargetTeamId() throws Exception {
		mockMvc.perform(patch(BASE_URL, 1L, STUDENT_NUMBER)
				.principal(authentication)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetTeamId\":0}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("projectRole이 50자를 넘으면 400을 응답한다")
	void rejectsTooLongProjectRole() throws Exception {
		String tooLong = "역".repeat(51);

		mockMvc.perform(patch(BASE_URL, 1L, STUDENT_NUMBER)
				.principal(authentication)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
					new TeamMemberUpdateRequest(null, tooLong, null))))
			.andExpect(status().isBadRequest());
	}
}
