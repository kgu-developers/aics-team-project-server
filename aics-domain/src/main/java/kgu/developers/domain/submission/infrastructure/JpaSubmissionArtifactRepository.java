package kgu.developers.domain.submission.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubmissionArtifactRepository extends JpaRepository<SubmissionArtifactJpaEntity, Long> {
    List<SubmissionArtifactJpaEntity> findAllByVersionId(Long versionId);
}
