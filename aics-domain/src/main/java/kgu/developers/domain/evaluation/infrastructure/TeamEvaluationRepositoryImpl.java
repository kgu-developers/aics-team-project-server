package kgu.developers.domain.evaluation.infrastructure;

import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeamEvaluationRepositoryImpl implements TeamEvaluationRepository {
    private final JpaTeamEvaluationRepository jpaRepository;

    @Override
    public TeamEvaluation save(TeamEvaluation evaluation) {
        return jpaRepository.save(TeamEvaluationJpaEntity.toEntity(evaluation)).toDomain();
    }

    @Override
    public Optional<TeamEvaluation> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(TeamEvaluationJpaEntity::toDomain);
    }

    @Override
    public Optional<TeamEvaluation> findByMilestoneIdAndRaterIdAndRateeTeamId(
            Long milestoneId,
            String raterId,
            Long rateeTeamId
    ) {
        return jpaRepository.findByMilestoneIdAndRaterIdAndRateeTeamIdAndDeletedAtIsNull(
                        milestoneId,
                        normalizeRaterId(raterId),
                        rateeTeamId
                )
                .map(TeamEvaluationJpaEntity::toDomain);
    }

    @Override
    public List<TeamEvaluation> findAllByMilestoneIdAndRaterId(Long milestoneId, String raterId) {
        return jpaRepository.findAllByMilestoneIdAndRaterIdAndDeletedAtIsNull(
                        milestoneId,
                        normalizeRaterId(raterId)
                ).stream()
                .map(TeamEvaluationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<TeamEvaluation> findAllByMilestoneId(Long milestoneId) {
        return jpaRepository.findAllByMilestoneIdAndDeletedAtIsNull(milestoneId).stream()
                .map(TeamEvaluationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<TeamEvaluation> findAllByMilestoneIdAndRateeTeamId(Long milestoneId, Long rateeTeamId) {
        return jpaRepository.findAllByMilestoneIdAndRateeTeamIdAndDeletedAtIsNull(milestoneId, rateeTeamId).stream()
                .map(TeamEvaluationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<TeamEvaluation> findAllByMilestoneIdAndRateeTeamIdIn(Long milestoneId, List<Long> rateeTeamIds) {
        if (rateeTeamIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByMilestoneIdAndRateeTeamIdInAndDeletedAtIsNull(milestoneId, rateeTeamIds).stream()
                .map(TeamEvaluationJpaEntity::toDomain)
                .toList();
    }

    private static String normalizeRaterId(String raterId) {
        return raterId == null ? null : raterId.trim();
    }
}
