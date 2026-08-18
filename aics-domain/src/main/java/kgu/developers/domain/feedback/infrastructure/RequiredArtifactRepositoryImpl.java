package kgu.developers.domain.feedback.infrastructure;

import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RequiredArtifactRepositoryImpl implements RequiredArtifactRepository {
    private final JpaRequiredArtifactRepository jpaRepository;

    @Override
    public RequiredArtifact save(RequiredArtifact requiredArtifact) {
        return jpaRepository.save(RequiredArtifactJpaEntity.toEntity(requiredArtifact)).toDomain();
    }

    @Override
    public Optional<RequiredArtifact> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(RequiredArtifactJpaEntity::toDomain);
    }

    @Override
    public List<RequiredArtifact> findAllByMilestoneId(Long milestoneId) {
        return jpaRepository.findAllByMilestoneIdAndDeletedAtIsNull(milestoneId).stream()
                .map(RequiredArtifactJpaEntity::toDomain)
                .toList();
    }
}
