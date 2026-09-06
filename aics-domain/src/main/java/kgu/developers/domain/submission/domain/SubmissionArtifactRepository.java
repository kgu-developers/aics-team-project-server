package kgu.developers.domain.submission.domain;

import java.util.List;

public interface SubmissionArtifactRepository {
    List<SubmissionArtifact> saveAll(List<SubmissionArtifact> artifacts);

    List<SubmissionArtifact> findAllByVersionId(Long versionId);

    List<SubmissionArtifact> findAllByVersionIdIn(List<Long> versionIds);
}
