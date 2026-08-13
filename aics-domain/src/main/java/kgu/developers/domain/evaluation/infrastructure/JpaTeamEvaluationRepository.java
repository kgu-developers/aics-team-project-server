package kgu.developers.domain.evaluation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTeamEvaluationRepository extends JpaRepository<TeamEvaluationJpaEntity, Long> {
    Optional<TeamEvaluationJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<TeamEvaluationJpaEntity> findByMilestoneIdAndRaterIdAndRateeTeamIdAndDeletedAtIsNull(
            Long milestoneId,
            String raterId,
            Long rateeTeamId
    );

    List<TeamEvaluationJpaEntity> findAllByMilestoneIdAndRaterIdAndDeletedAtIsNull(Long milestoneId, String raterId);
}
