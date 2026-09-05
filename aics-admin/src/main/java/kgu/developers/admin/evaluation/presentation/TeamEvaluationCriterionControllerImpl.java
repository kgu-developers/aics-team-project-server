package kgu.developers.admin.evaluation.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.evaluation.application.TeamEvaluationCriterionFacade;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionListResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionPersistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sections/{sectionId}/team-evaluation-criteria")
public class TeamEvaluationCriterionControllerImpl implements TeamEvaluationCriterionController {

  private final TeamEvaluationCriterionFacade facade;

  @Override
  @GetMapping
  public ResponseEntity<TeamEvaluationCriterionListResponse> getCriteria(
      @Positive @PathVariable Long sectionId,
      Authentication authentication) {
    return ResponseEntity.ok(facade.getCriteria(sectionId, authentication.getName()));
  }

  @Override
  @PostMapping
  public ResponseEntity<TeamEvaluationCriterionPersistResponse> createCriterion(
      @Positive @PathVariable Long sectionId,
      @Valid @RequestBody TeamEvaluationCriterionCreateRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(facade.createCriterion(sectionId, authentication.getName(), request));
  }
}
