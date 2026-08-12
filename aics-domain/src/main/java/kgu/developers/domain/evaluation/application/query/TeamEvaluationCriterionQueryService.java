package kgu.developers.domain.evaluation.application.query;

import java.util.List;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamEvaluationCriterionQueryService {

  private final TeamEvaluationCriterionRepository criterionRepository;

  public List<TeamEvaluationCriterion> getCriteria(Long sectionId) {
    return criterionRepository.findAllBySectionIdOrderByDisplayOrder(sectionId);
  }
}
