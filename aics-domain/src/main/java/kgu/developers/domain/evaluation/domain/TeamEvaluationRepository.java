package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface TeamEvaluationRepository {
    TeamEvaluation save(TeamEvaluation evaluation);

    Optional<TeamEvaluation> findById(Long id);

    Optional<TeamEvaluation> findByMilestoneIdAndRaterIdAndRateeTeamId(Long milestoneId, String raterId, Long rateeTeamId);

    List<TeamEvaluation> findAllByMilestoneIdAndRaterId(Long milestoneId, String raterId);
}
