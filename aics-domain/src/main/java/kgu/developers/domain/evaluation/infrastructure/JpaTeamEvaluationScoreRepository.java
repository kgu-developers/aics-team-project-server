package kgu.developers.domain.evaluation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaTeamEvaluationScoreRepository extends JpaRepository<TeamEvaluationScoreJpaEntity, Long> {
    Optional<TeamEvaluationScoreJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TeamEvaluationScoreJpaEntity> findAllByTeamEvaluationIdAndDeletedAtIsNull(Long teamEvaluationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TeamEvaluationScoreJpaEntity e WHERE e.teamEvaluationId = :teamEvaluationId")
    void deleteAllByTeamEvaluationId(@Param("teamEvaluationId") Long teamEvaluationId);
}
