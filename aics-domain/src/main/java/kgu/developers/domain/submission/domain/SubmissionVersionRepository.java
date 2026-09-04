package kgu.developers.domain.submission.domain;

import java.util.List;
import java.util.Optional;

public interface SubmissionVersionRepository {
    SubmissionVersion save(SubmissionVersion submissionVersion);

    List<SubmissionVersion> findAllBySubmissionId(Long submissionId);

    Optional<SubmissionVersion> findBySubmissionIdAndVersion(Long submissionId, int version);

    int countBySubmissionId(Long submissionId);
}
