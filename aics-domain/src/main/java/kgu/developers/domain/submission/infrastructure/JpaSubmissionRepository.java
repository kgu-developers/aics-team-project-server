package kgu.developers.domain.submission.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubmissionRepository extends JpaRepository<SubmissionJpaEntity, Long> {
    Optional<SubmissionJpaEntity> findByTeamIdAndMilestoneId(Long teamId, Long milestoneId);

    List<SubmissionJpaEntity> findAllByMilestoneId(Long milestoneId);
}
