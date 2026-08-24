package kgu.developers.api.team.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.team.application.TeamFacade;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/oop")
public class TeamControllerImpl implements TeamController {

	private final TeamFacade teamFacade;

	@Override
	@GetMapping("/teams/{teamId}/kickoff")
	public ResponseEntity<TeamKickoffResponse> getKickoffByTeamId(
			@Positive @PathVariable Long teamId, Authentication authentication) {
		return ResponseEntity.ok(teamFacade.getKickoffByTeamId(teamId, authentication.getName()));
	}

	@Override
	@PutMapping("/teams/{teamId}/kickoff")
	public ResponseEntity<TeamKickoffResponse> updateKickoff(
			@Positive @PathVariable Long teamId,
			@Valid @RequestBody TeamKickoffUpdateRequest request,
			Authentication authentication) {
		return ResponseEntity.ok(teamFacade.updateKickoff(teamId, authentication.getName(), request));
	}

	@Override
	@PostMapping("/teams/{teamId}/leader-claim")
	public ResponseEntity<Void> claimLeader(
			@Positive @PathVariable Long teamId, Authentication authentication) {
		teamFacade.claimLeader(teamId, authentication.getName());
		return ResponseEntity.noContent().build();
	}
}
