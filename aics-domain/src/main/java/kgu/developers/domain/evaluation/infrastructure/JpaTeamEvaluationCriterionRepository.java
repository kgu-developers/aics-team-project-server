package kgu.developers.domain.evaluation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTeamEvaluationCriterionRepository extends JpaRepository<TeamEvaluationCriterionJpaEntity, Long> {
    Optional<TeamEvaluationCriterionJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TeamEvaluationCriterionJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long sectionId);
}
