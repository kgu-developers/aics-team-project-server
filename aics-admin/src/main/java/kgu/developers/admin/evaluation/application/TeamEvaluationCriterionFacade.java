package kgu.developers.admin.evaluation.application;

import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionUpdateRequest;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionListResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionPersistResponse;
import kgu.developers.domain.evaluation.application.command.TeamEvaluationCriterionCommandService;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScoreRepository;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScoreRepository;
import kgu.developers.domain.evaluation.exception.TeamEvaluationCriterionLockedException;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.evaluation.exception.TeamEvaluationCriterionNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
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
  private final SectionRepository sectionRepository;
  private final MilestoneRepository milestoneRepository;
  private final TeamEvaluationScoreRepository scoreRepository;
  private final ProfessorPresentationEvaluationScoreRepository professorScoreRepository;
  private final Clock serviceClock;

  @Transactional
  public TeamEvaluationCriterionPersistResponse createCriterion(
      Long sectionId, String professorId, TeamEvaluationCriterionCreateRequest request) {
    lockSectionAccess(sectionId, professorId);
    validateNotStarted(sectionId);
    boolean hasScores = queryService.getCriteria(sectionId).stream()
        .anyMatch(criterion -> scoreRepository.existsByCriterionId(criterion.getId())
            || professorScoreRepository.existsByCriterionId(criterion.getId()));
    if (hasScores) {
      throw new TeamEvaluationCriterionLockedException();
    }
    Long id = commandService.createCriterion(
        sectionId, request.title(), request.maxScore(), request.displayOrder());
    return TeamEvaluationCriterionPersistResponse.of(id);
  }

  public TeamEvaluationCriterionListResponse getCriteria(Long sectionId, String professorId) {
    validateSectionAccess(sectionId, professorId);
    return TeamEvaluationCriterionListResponse.from(queryService.getCriteria(sectionId));
  }

  @Transactional
  public void updateCriterion(Long sectionId, Long criterionId, String professorId,
      TeamEvaluationCriterionUpdateRequest request) {
    lockSectionAccess(sectionId, professorId);
    TeamEvaluationCriterion criterion = ownedCriterion(sectionId, criterionId);
    validateNotStarted(sectionId);
    validateNoScores(criterionId);
    commandService.updateCriterion(criterion, request.title(), request.maxScore(), request.displayOrder());
  }

  @Transactional
  public void deleteCriterion(Long sectionId, Long criterionId, String professorId) {
    lockSectionAccess(sectionId, professorId);
    TeamEvaluationCriterion criterion = ownedCriterion(sectionId, criterionId);
    validateNotStarted(sectionId);
    validateNoScores(criterionId);
    commandService.deleteCriterion(criterion, LocalDateTime.now(serviceClock));
  }

  private TeamEvaluationCriterion ownedCriterion(Long sectionId, Long criterionId) {
    TeamEvaluationCriterion criterion = queryService.getCriterion(criterionId);
    if (!criterion.getSectionId().equals(sectionId)) {
      throw new TeamEvaluationCriterionNotFoundException();
    }
    return criterion;
  }

  private void validateNotStarted(Long sectionId) {
    LocalDateTime now = LocalDateTime.now(serviceClock);
    boolean started = milestoneRepository.findAllBySectionIdOrderByWeekNumber(sectionId).stream()
        .filter(milestone -> milestone.getType() == MilestoneType.PRESENTATION)
        .map(milestone -> milestone.getSchedule().evaluationOpensAt())
        .anyMatch(opensAt -> opensAt != null && !opensAt.isAfter(now));
    if (started) {
      throw new TeamEvaluationCriterionLockedException();
    }
  }

  private void validateNoScores(Long criterionId) {
    if (scoreRepository.existsByCriterionId(criterionId)
        || professorScoreRepository.existsByCriterionId(criterionId)) {
      throw new TeamEvaluationCriterionLockedException();
    }
  }

  private void lockSectionAccess(Long sectionId, String professorId) {
    if (!sectionRepository.lockActiveByIdAndProfessorId(sectionId, professorId)) {
      throw new AccessDeniedException("담당 분반의 발표 평가 항목만 관리할 수 있습니다.");
    }
  }

  private void validateSectionAccess(Long sectionId, String professorId) {
    if (!sectionQueryService.isActiveSectionOwnedByProfessor(sectionId, professorId)) {
      throw new AccessDeniedException("담당 분반의 발표 평가 항목만 관리할 수 있습니다.");
    }
  }
}
