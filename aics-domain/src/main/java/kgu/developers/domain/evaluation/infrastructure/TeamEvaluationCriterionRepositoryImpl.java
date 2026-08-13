package kgu.developers.domain.evaluation.infrastructure;

import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeamEvaluationCriterionRepositoryImpl implements TeamEvaluationCriterionRepository {
    private final JpaTeamEvaluationCriterionRepository jpaRepository;

    @Override
    public TeamEvaluationCriterion save(TeamEvaluationCriterion criterion) {
        return jpaRepository.save(TeamEvaluationCriterionJpaEntity.toEntity(criterion)).toDomain();
    }

    @Override
    public Optional<TeamEvaluationCriterion> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(TeamEvaluationCriterionJpaEntity::toDomain);
    }

    @Override
    public List<TeamEvaluationCriterion> findAllBySectionIdOrderByDisplayOrder(Long sectionId) {
        return jpaRepository.findAllBySectionIdAndDeletedAtIsNullOrderByDisplayOrderAsc(sectionId).stream()
                .map(TeamEvaluationCriterionJpaEntity::toDomain)
                .toList();
    }
}
