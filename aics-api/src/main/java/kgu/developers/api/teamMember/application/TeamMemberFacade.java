package kgu.developers.api.teamMember.application;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactResponse;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamMemberFacade {
	private final TeamQueryService teamQueryService;
	private final TeamMemberQueryService teamMemberQueryService;
	private final TeamAccessValidator teamAccessValidator;

	public TeamMemberContactListResponse getContacts(Long teamId, String userId) {
		teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
		teamQueryService.validateContactVisible(teamId);

		return new TeamMemberContactListResponse(
			teamMemberQueryService.getTeamMembersWithUsers(teamId).stream()
				.map(it -> TeamMemberContactResponse.of(it.member(), it.user()))
				.toList());
	}
}
