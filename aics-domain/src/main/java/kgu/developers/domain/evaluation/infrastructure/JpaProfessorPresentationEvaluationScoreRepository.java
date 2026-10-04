package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProfessorPresentationEvaluationScoreRepository
        extends JpaRepository<ProfessorPresentationEvaluationScoreJpaEntity, Long> {
    List<ProfessorPresentationEvaluationScoreJpaEntity> findAllByEvaluationId(Long evaluationId);
    boolean existsByCriterionId(Long criterionId);
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM ProfessorPresentationEvaluationScoreJpaEntity score WHERE score.evaluationId = :evaluationId")
    void deleteAllByEvaluationId(@Param("evaluationId") Long evaluationId);
}
