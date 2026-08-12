package kgu.developers.domain.feedback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaRequiredArtifactRepository extends JpaRepository<RequiredArtifactJpaEntity, Long> {
    Optional<RequiredArtifactJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<RequiredArtifactJpaEntity> findAllByMilestoneIdAndDeletedAtIsNull(Long milestoneId);
}
