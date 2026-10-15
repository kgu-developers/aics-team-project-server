package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluation;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfessorPresentationEvaluationRepositoryImpl
        implements ProfessorPresentationEvaluationRepository {
    private final JpaProfessorPresentationEvaluationRepository jpaRepository;

    @Override
    public ProfessorPresentationEvaluation save(ProfessorPresentationEvaluation evaluation) {
        return jpaRepository.save(ProfessorPresentationEvaluationJpaEntity.fromDomain(evaluation)).toDomain();
    }

    @Override
    public Optional<ProfessorPresentationEvaluation> findByMilestoneIdAndTeamId(Long milestoneId, Long teamId) {
        return jpaRepository.findByMilestoneIdAndTeamIdAndDeletedAtIsNull(milestoneId, teamId)
                .map(ProfessorPresentationEvaluationJpaEntity::toDomain);
    }

    @Override
    public List<ProfessorPresentationEvaluation> findAllByMilestoneId(Long milestoneId) {
        return jpaRepository.findAllByMilestoneIdAndDeletedAtIsNull(milestoneId).stream()
                .map(ProfessorPresentationEvaluationJpaEntity::toDomain)
                .toList();
    }
}
