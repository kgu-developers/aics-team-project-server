package kgu.developers.domain.submission.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubmissionRepositoryImpl implements SubmissionRepository {
    private final JpaSubmissionRepository jpaSubmissionRepository;

    @Override
    public Submission save(Submission submission) {
        SubmissionJpaEntity entity = SubmissionJpaEntity.fromDomain(submission);
        return jpaSubmissionRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Submission> findById(Long id) {
        return jpaSubmissionRepository.findById(id)
                .map(SubmissionJpaEntity::toDomain);
    }

    @Override
    public Optional<Submission> findByTeamIdAndMilestoneId(Long teamId, Long milestoneId) {
        return jpaSubmissionRepository.findByTeamIdAndMilestoneId(teamId, milestoneId)
                .map(SubmissionJpaEntity::toDomain);
    }

    @Override
    public List<Submission> findAllByMilestoneId(Long milestoneId) {
        return jpaSubmissionRepository.findAllByMilestoneId(milestoneId).stream()
                .map(SubmissionJpaEntity::toDomain)
                .toList();
    }
}
