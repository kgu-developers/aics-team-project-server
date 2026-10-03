package kgu.developers.domain.evaluation.domain;

import java.util.List;

public interface ProfessorPresentationEvaluationScoreRepository {
    List<ProfessorPresentationEvaluationScore> findAllByEvaluationId(Long evaluationId);
    boolean existsByCriterionId(Long criterionId);
    void replaceAll(Long evaluationId, List<ProfessorPresentationEvaluationScore> scores);
}
