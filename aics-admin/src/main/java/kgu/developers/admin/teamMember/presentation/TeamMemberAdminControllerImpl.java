package kgu.developers.admin.teamMember.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.teamMember.application.TeamMemberAdminFacade;
import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/teams/{teamId}/members")
public class TeamMemberAdminControllerImpl implements TeamMemberAdminController {

	private final TeamMemberAdminFacade teamMemberAdminFacade;

	@Override
	@PatchMapping("/{studentNumber}")
	public ResponseEntity<TeamMemberAdminResponse> updateTeamMember(
			@Positive @PathVariable Long teamId,
			@PathVariable String studentNumber,
			@Valid @RequestBody TeamMemberUpdateRequest request,
			Authentication authentication) {
		return ResponseEntity.ok(teamMemberAdminFacade.updateTeamMember(
			teamId, studentNumber, request, authentication.getName()));
	}
}
