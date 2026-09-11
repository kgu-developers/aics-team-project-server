package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface TeamEvaluationScoreRepository {
    TeamEvaluationScore save(TeamEvaluationScore score);

    List<TeamEvaluationScore> saveAll(List<TeamEvaluationScore> scores);

    Optional<TeamEvaluationScore> findById(Long id);

    List<TeamEvaluationScore> findAllByTeamEvaluationId(Long teamEvaluationId);

    List<TeamEvaluationScore> findAllByTeamEvaluationIds(List<Long> teamEvaluationIds);

    void deleteAllByTeamEvaluationId(Long teamEvaluationId);
}
