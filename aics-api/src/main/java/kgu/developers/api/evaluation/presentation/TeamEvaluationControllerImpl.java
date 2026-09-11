package kgu.developers.api.evaluation.presentation;

import jakarta.validation.Valid;
import kgu.developers.api.evaluation.application.TeamEvaluationFacade;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationSubmitRequest;
import kgu.developers.api.evaluation.presentation.response.MyTeamEvaluationsResponse;
import kgu.developers.api.evaluation.presentation.response.TeamEvaluationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/milestones/{milestoneId}/team-evaluations")
@RequiredArgsConstructor
public class TeamEvaluationControllerImpl implements TeamEvaluationController {
    private final TeamEvaluationFacade facade;

    @Override
    @GetMapping("/me")
    public ResponseEntity<MyTeamEvaluationsResponse> getMyEvaluations(
            @PathVariable Long milestoneId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(facade.getMyEvaluations(milestoneId, authentication.getName()));
    }

    @Override
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamEvaluationResponse> submit(
            @PathVariable Long milestoneId,
            @PathVariable Long teamId,
            @Valid @RequestBody TeamEvaluationSubmitRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(facade.submit(milestoneId, teamId, authentication.getName(), request));
    }
}
