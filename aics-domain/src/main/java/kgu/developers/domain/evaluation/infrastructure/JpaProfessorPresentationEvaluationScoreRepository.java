package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfessorPresentationEvaluationScoreRepository
        extends JpaRepository<ProfessorPresentationEvaluationScoreJpaEntity, Long> {
    List<ProfessorPresentationEvaluationScoreJpaEntity> findAllByEvaluationId(Long evaluationId);
    boolean existsByCriterionId(Long criterionId);
    void deleteAllByEvaluationId(Long evaluationId);
}
