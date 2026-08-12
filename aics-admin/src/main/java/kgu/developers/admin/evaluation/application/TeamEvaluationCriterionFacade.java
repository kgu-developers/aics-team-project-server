package kgu.developers.admin.evaluation.application;

import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionListResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionPersistResponse;
import kgu.developers.domain.evaluation.application.command.TeamEvaluationCriterionCommandService;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamEvaluationCriterionFacade {

  private final TeamEvaluationCriterionCommandService commandService;
  private final TeamEvaluationCriterionQueryService queryService;

  public TeamEvaluationCriterionPersistResponse createCriterion(
      Long sectionId, TeamEvaluationCriterionCreateRequest request) {
    Long id = commandService.createCriterion(
        sectionId, request.title(), request.maxScore(), request.displayOrder());
    return TeamEvaluationCriterionPersistResponse.of(id);
  }

  public TeamEvaluationCriterionListResponse getCriteria(Long sectionId) {
    return TeamEvaluationCriterionListResponse.from(queryService.getCriteria(sectionId));
  }
}
