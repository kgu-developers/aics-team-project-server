package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScore;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfessorPresentationEvaluationScoreRepositoryImpl
        implements ProfessorPresentationEvaluationScoreRepository {
    private final JpaProfessorPresentationEvaluationScoreRepository jpaRepository;

    @Override
    public List<ProfessorPresentationEvaluationScore> findAllByEvaluationId(Long evaluationId) {
        return jpaRepository.findAllByEvaluationId(evaluationId).stream()
                .map(ProfessorPresentationEvaluationScoreJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsByCriterionId(Long criterionId) {
        return jpaRepository.existsByCriterionId(criterionId);
    }

    @Override
    public void replaceAll(Long evaluationId, List<ProfessorPresentationEvaluationScore> scores) {
        jpaRepository.deleteAllByEvaluationId(evaluationId);
        jpaRepository.flush();
        jpaRepository.saveAll(scores.stream()
                .map(ProfessorPresentationEvaluationScoreJpaEntity::fromDomain).toList());
    }
}
