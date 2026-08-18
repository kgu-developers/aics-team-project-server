package kgu.developers.domain.evaluation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTeamEvaluationScoreRepository extends JpaRepository<TeamEvaluationScoreJpaEntity, Long> {
    Optional<TeamEvaluationScoreJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TeamEvaluationScoreJpaEntity> findAllByTeamEvaluationIdAndDeletedAtIsNull(Long teamEvaluationId);
}
