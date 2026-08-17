package kgu.developers.admin.team.presentation;

import jakarta.validation.constraints.Positive;
import kgu.developers.admin.team.application.TeamAdminFacade;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop")
public class TeamAdminControllerImpl implements TeamAdminController {

	private final TeamAdminFacade teamAdminFacade;

	@Override
	@GetMapping("/teams/{teamId}")
	public ResponseEntity<TeamAdminDetailResponse> getTeamById(
			@Positive @PathVariable Long teamId) {
		return ResponseEntity.ok(teamAdminFacade.getTeamById(teamId));
	}

	@Override
	@PatchMapping("/sections/{sectionId}/teams/finalize")
	public ResponseEntity<TeamAdminListResponse> finalizeTeams(
			@Positive @PathVariable Long sectionId) {
		return ResponseEntity.ok(teamAdminFacade.finalizeTeams(sectionId));
	}
}
