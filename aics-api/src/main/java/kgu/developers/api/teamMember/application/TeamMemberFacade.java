package kgu.developers.api.teamMember.application;

import java.util.List;
import java.util.Map;

import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberListResponse;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;
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

	public TeamMemberListResponse getTeamMembers(Long teamId, String userId, String keyword) {
		teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
		return new TeamMemberListResponse(teamMemberQueryService.getTeamMembersWithUsers(teamId).stream()
			.filter(it -> it.member().getUserId().contains(keyword)
				|| it.user() != null && it.user().getName().contains(keyword))
			.map(it -> TeamMemberResponse.of(it.member(), it.user()))
			.toList());
	}
}
