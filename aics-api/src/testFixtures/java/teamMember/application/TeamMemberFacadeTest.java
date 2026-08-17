package teamMember.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.api.teamMember.application.TeamMemberFacade;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import kgu.developers.domain.section.exception.ContactNotVisibleException;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;

@ExtendWith(MockitoExtension.class)
class TeamMemberFacadeTest {

	@Mock
	private TeamQueryService teamQueryService;

	@Mock
	private TeamMemberQueryService teamMemberQueryService;

	@Mock
	private UserQueryService userQueryService;

	@InjectMocks
	private TeamMemberFacade teamMemberFacade;

	private TeamMember member() {
		return TeamMember.builder()
			.id(1L).teamId(1L).userId("202699999").isLeader(false).projectRole("백엔드").build();
	}

	@Test
	@DisplayName("공개기간 안이면 팀원 연락처를 응답한다")
	void getContacts() {
		given(teamMemberQueryService.getTeamMembersByTeamId(1L)).willReturn(List.of(member()));
		given(userQueryService.getUsersByStudentNumbers(List.of("202699999"))).willReturn(List.of(
			User.builder().studentNumber("202699999").name("김철수")
				.email("kim@kgu.ac.kr").phone("010-0000-0001").build()));

		TeamMemberContactListResponse response = teamMemberFacade.getContacts(1L);

		assertThat(response.contents()).singleElement().satisfies(contact -> {
			assertThat(contact.studentNumber()).isEqualTo("202699999");
			assertThat(contact.email()).isEqualTo("kim@kgu.ac.kr");
			assertThat(contact.phone()).isEqualTo("010-0000-0001");
		});
	}

	@Test
	@DisplayName("사용자가 조회되지 않아도 학번만 담아 응답한다")
	void getContactsWithMissingUser() {
		given(teamMemberQueryService.getTeamMembersByTeamId(1L)).willReturn(List.of(member()));
		given(userQueryService.getUsersByStudentNumbers(List.of("202699999"))).willReturn(List.of());

		TeamMemberContactListResponse response = teamMemberFacade.getContacts(1L);

		assertThat(response.contents()).singleElement().satisfies(contact -> {
			assertThat(contact.studentNumber()).isEqualTo("202699999");
			assertThat(contact.email()).isNull();
			assertThat(contact.phone()).isNull();
		});
	}

	@Test
	@DisplayName("공개기간 밖이면 팀원을 조회하지 않고 예외를 던진다")
	void rejectsContactsOutsidePeriod() {
		willThrow(new ContactNotVisibleException()).given(teamQueryService).validateContactVisible(1L);

		assertThatThrownBy(() -> teamMemberFacade.getContacts(1L))
			.isInstanceOf(ContactNotVisibleException.class);

		verify(teamMemberQueryService, never()).getTeamMembersByTeamId(1L);
	}
}
