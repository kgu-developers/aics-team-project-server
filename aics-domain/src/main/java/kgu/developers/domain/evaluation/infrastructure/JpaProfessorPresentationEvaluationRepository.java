package kgu.developers.domain.evaluation.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfessorPresentationEvaluationRepository
        extends JpaRepository<ProfessorPresentationEvaluationJpaEntity, Long> {
    Optional<ProfessorPresentationEvaluationJpaEntity> findByMilestoneIdAndTeamIdAndDeletedAtIsNull(
            Long milestoneId, Long teamId);
}
