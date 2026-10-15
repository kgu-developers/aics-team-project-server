package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface ProfessorPresentationEvaluationRepository {
    ProfessorPresentationEvaluation save(ProfessorPresentationEvaluation evaluation);
    Optional<ProfessorPresentationEvaluation> findByMilestoneIdAndTeamId(Long milestoneId, Long teamId);
    List<ProfessorPresentationEvaluation> findAllByMilestoneId(Long milestoneId);
}
