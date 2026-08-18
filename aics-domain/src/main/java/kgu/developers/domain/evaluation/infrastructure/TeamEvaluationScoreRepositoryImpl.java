package kgu.developers.domain.evaluation.infrastructure;

import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeamEvaluationScoreRepositoryImpl implements TeamEvaluationScoreRepository {
    private final JpaTeamEvaluationScoreRepository jpaRepository;

    @Override
    public TeamEvaluationScore save(TeamEvaluationScore score) {
        return jpaRepository.save(TeamEvaluationScoreJpaEntity.toEntity(score)).toDomain();
    }

    @Override
    public Optional<TeamEvaluationScore> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(TeamEvaluationScoreJpaEntity::toDomain);
    }

    @Override
    public List<TeamEvaluationScore> findAllByTeamEvaluationId(Long teamEvaluationId) {
        return jpaRepository.findAllByTeamEvaluationIdAndDeletedAtIsNull(teamEvaluationId).stream()
                .map(TeamEvaluationScoreJpaEntity::toDomain)
                .toList();
    }
}
