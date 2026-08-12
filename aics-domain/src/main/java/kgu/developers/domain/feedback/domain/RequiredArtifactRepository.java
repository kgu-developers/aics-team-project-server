package kgu.developers.domain.feedback.domain;

import java.util.List;
import java.util.Optional;

public interface RequiredArtifactRepository {
    RequiredArtifact save(RequiredArtifact requiredArtifact);

    Optional<RequiredArtifact> findById(Long id);

    List<RequiredArtifact> findAllByMilestoneId(Long milestoneId);
}
