package kgu.developers.domain.submission.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubmissionArtifactRepository extends JpaRepository<SubmissionArtifactJpaEntity, Long> {
    List<SubmissionArtifactJpaEntity> findAllByVersionIdAndDeletedAtIsNull(Long versionId);

    List<SubmissionArtifactJpaEntity> findAllByVersionIdInAndDeletedAtIsNull(List<Long> versionIds);
}
