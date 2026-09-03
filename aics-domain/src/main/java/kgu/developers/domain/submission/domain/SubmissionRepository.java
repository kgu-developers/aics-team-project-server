package kgu.developers.domain.submission.domain;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository {
    Submission save(Submission submission);

    Optional<Submission> findById(Long id);

    /** 상태 전환(제출·완료·재개)과 버전 채번을 같은 잠금 규약으로 직렬화하기 위해 쓴다. */
    Optional<Submission> findByIdForUpdate(Long id);

    Optional<Submission> findByTeamIdAndMilestoneId(Long teamId, Long milestoneId);

    List<Submission> findAllByMilestoneId(Long milestoneId);
}
