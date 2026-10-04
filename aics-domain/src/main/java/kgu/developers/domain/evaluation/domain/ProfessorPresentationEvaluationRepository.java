package kgu.developers.domain.evaluation.domain;

import java.util.Optional;

public interface ProfessorPresentationEvaluationRepository {
    ProfessorPresentationEvaluation save(ProfessorPresentationEvaluation evaluation);
    Optional<ProfessorPresentationEvaluation> findByMilestoneIdAndTeamId(Long milestoneId, Long teamId);
}
