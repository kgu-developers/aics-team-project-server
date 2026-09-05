package kgu.developers.api.teamMember.presentation;

import jakarta.validation.constraints.Positive;
import kgu.developers.api.teamMember.application.TeamMemberFacade;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams/{teamId}/members")
public class TeamMemberControllerImpl implements TeamMemberController {

	private final TeamMemberFacade teamMemberFacade;

	@Override
	@GetMapping("/contacts")
	public ResponseEntity<TeamMemberContactListResponse> getContacts(
			@Positive @PathVariable Long teamId, Authentication authentication) {
		return ResponseEntity.ok(teamMemberFacade.getContacts(teamId, authentication.getName()));
	}
}
