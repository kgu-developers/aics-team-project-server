package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.evaluation.domain.Grade;
import kgu.developers.domain.evaluation.domain.GradeRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GradeRepositoryImpl implements GradeRepository {
    private final JpaGradeRepository jpaRepository;

    @Override
    public Grade save(Grade grade) {
        return jpaRepository.save(GradeJpaEntity.toEntity(grade)).toDomain();
    }

    @Override
    public Optional<Grade> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(GradeJpaEntity::toDomain);
    }

    @Override
    public Optional<Grade> findBySectionIdAndTeamIdAndUserId(Long sectionId, Long teamId, String userId) {
        return jpaRepository.findBySectionIdAndTeamIdAndUserIdAndDeletedAtIsNull(
                        sectionId,
                        teamId,
                        userId == null ? null : userId.trim()
                )
                .map(GradeJpaEntity::toDomain);
    }

    @Override
    public List<Grade> findAllBySectionIdAndTeamId(Long sectionId, Long teamId) {
        return jpaRepository.findAllBySectionIdAndTeamIdAndDeletedAtIsNull(sectionId, teamId)
                .stream()
                .map(GradeJpaEntity::toDomain)
                .toList();
    }

}
