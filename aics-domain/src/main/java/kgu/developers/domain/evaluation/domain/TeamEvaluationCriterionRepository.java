package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface TeamEvaluationCriterionRepository {
    TeamEvaluationCriterion save(TeamEvaluationCriterion criterion);

    Optional<TeamEvaluationCriterion> findById(Long id);

    List<TeamEvaluationCriterion> findAllBySectionIdOrderByDisplayOrder(Long sectionId);
}
