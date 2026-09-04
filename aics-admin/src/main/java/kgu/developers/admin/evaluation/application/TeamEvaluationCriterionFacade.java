package kgu.developers.admin.evaluation.application;

import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionListResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionPersistResponse;
import kgu.developers.domain.evaluation.application.command.TeamEvaluationCriterionCommandService;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamEvaluationCriterionFacade {

  private final TeamEvaluationCriterionCommandService commandService;
  private final TeamEvaluationCriterionQueryService queryService;
  private final SectionQueryService sectionQueryService;

  @Transactional
  public TeamEvaluationCriterionPersistResponse createCriterion(
      Long sectionId, String professorId, TeamEvaluationCriterionCreateRequest request) {
    validateSectionAccess(sectionId, professorId);
    Long id = commandService.createCriterion(
        sectionId, request.title(), request.maxScore(), request.displayOrder());
    return TeamEvaluationCriterionPersistResponse.of(id);
  }

  public TeamEvaluationCriterionListResponse getCriteria(Long sectionId, String professorId) {
    validateSectionAccess(sectionId, professorId);
    return TeamEvaluationCriterionListResponse.from(queryService.getCriteria(sectionId));
  }

  private void validateSectionAccess(Long sectionId, String professorId) {
    if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
      throw new AccessDeniedException("담당 분반의 발표 평가 항목만 관리할 수 있습니다.");
    }
  }
}
