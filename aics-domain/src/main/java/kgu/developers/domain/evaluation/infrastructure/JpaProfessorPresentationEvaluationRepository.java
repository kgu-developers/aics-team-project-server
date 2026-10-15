package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfessorPresentationEvaluationRepository
        extends JpaRepository<ProfessorPresentationEvaluationJpaEntity, Long> {
    Optional<ProfessorPresentationEvaluationJpaEntity> findByMilestoneIdAndTeamIdAndDeletedAtIsNull(
            Long milestoneId, Long teamId);
    List<ProfessorPresentationEvaluationJpaEntity> findAllByMilestoneIdAndDeletedAtIsNull(Long milestoneId);
}
