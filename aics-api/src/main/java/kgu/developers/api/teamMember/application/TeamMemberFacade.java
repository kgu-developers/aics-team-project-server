package kgu.developers.api.teamMember.application;

import java.util.Map;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactResponse;
import kgu.developers.domain.enrollment.application.query.EnrollmentQueryService;
import kgu.developers.domain.team.application.query.TeamQueryService;
import kgu.developers.domain.teamMember.application.query.TeamMemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamMemberFacade {
	private final TeamQueryService teamQueryService;
	private final TeamMemberQueryService teamMemberQueryService;
	private final EnrollmentQueryService enrollmentQueryService;
	private final TeamAccessValidator teamAccessValidator;

	public TeamMemberContactListResponse getContacts(Long teamId, String userId) {
		teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
		teamQueryService.validateContactVisible(teamId);

		Map<String, String> gradeOf = enrollmentQueryService
			.getGradesBySectionId(teamQueryService.getTeamById(teamId).getSectionId());

		return new TeamMemberContactListResponse(teamMemberQueryService.getTeamMembersWithUsers(teamId).stream()
			.map(it -> TeamMemberContactResponse.of(it.member(), it.user(), gradeOf.get(it.member().getUserId())))
			.toList());
	}
}
