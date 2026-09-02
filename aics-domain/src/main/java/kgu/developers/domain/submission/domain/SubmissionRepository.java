package kgu.developers.domain.submission.domain;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository {
    Submission save(Submission submission);

    Optional<Submission> findById(Long id);

    Optional<Submission> findByTeamIdAndMilestoneId(Long teamId, Long milestoneId);

    List<Submission> findAllByMilestoneId(Long milestoneId);
}
