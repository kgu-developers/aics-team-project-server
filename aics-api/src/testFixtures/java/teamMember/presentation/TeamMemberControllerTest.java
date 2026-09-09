package teamMember.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import kgu.developers.api.teamMember.application.TeamMemberFacade;
import kgu.developers.api.teamMember.presentation.TeamMemberControllerImpl;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;

@ExtendWith(MockitoExtension.class)
class TeamMemberControllerTest {

	private static final String BASE_URL = "/api/v1/oop/teams/{teamId}/members/contacts";
	private static final String MEMBERS_URL = "/api/v1/oop/teams/{teamId}/members";
	private static final String STUDENT_NUMBER = "202699999";

	@Mock
	private TeamMemberFacade teamMemberFacade;

	private MockMvc mockMvc;
	private final Authentication authentication =
		new UsernamePasswordAuthenticationToken(STUDENT_NUMBER, null);

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.standaloneSetup(new TeamMemberControllerImpl(teamMemberFacade))
			.build();
	}

	@Test
	@DisplayName("팀원 연락처를 조회하면 200과 이메일, 연락처를 응답한다")
	void getContacts() throws Exception {
		given(teamMemberFacade.getContacts(1L, STUDENT_NUMBER)).willReturn(
			new TeamMemberContactListResponse(List.of(new TeamMemberContactResponse(
				STUDENT_NUMBER, "김철수", "kim@kgu.ac.kr", "010-0000-0001", "3", true))));

		mockMvc.perform(get(BASE_URL, 1L).principal(authentication))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contents.length()").value(1))
			.andExpect(jsonPath("$.contents[0].studentNumber").value(STUDENT_NUMBER))
			.andExpect(jsonPath("$.contents[0].email").value("kim@kgu.ac.kr"))
			.andExpect(jsonPath("$.contents[0].phone").value("010-0000-0001"))
			.andExpect(jsonPath("$.contents[0].grade").value("3"))
			.andExpect(jsonPath("$.contents[0].isLeader").value(true));

		verify(teamMemberFacade).getContacts(1L, STUDENT_NUMBER);
	}

	@Test
	@DisplayName("팀원을 조회하면 본인을 포함해 200으로 응답한다")
	void getTeamMembers() throws Exception {
		given(teamMemberFacade.getTeamMembers(1L, STUDENT_NUMBER, "김")).willReturn(List.of(
			new TeamMemberResponse(1L, STUDENT_NUMBER, "김철수", true, "백엔드")));

		mockMvc.perform(get(MEMBERS_URL, 1L).param("keyword", "김").principal(authentication))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].studentNumber").value(STUDENT_NUMBER));

		verify(teamMemberFacade).getTeamMembers(1L, STUDENT_NUMBER, "김");
	}
}
