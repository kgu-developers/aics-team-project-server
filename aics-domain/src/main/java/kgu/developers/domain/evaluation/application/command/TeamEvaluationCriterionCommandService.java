package kgu.developers.domain.evaluation.application.command;

import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamEvaluationCriterionCommandService {

  private final TeamEvaluationCriterionRepository criterionRepository;

  public Long createCriterion(Long sectionId, String title, int maxScore, int displayOrder) {
    TeamEvaluationCriterion criterion =
        TeamEvaluationCriterion.create(sectionId, title, maxScore, displayOrder);
    return criterionRepository.save(criterion).getId();
  }
}
