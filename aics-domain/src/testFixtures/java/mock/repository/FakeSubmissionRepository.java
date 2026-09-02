package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.submission.domain.Submission;
import kgu.developers.domain.submission.domain.SubmissionRepository;

public class FakeSubmissionRepository implements SubmissionRepository {

    private final Map<Long, Submission> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public Submission save(Submission submission) {
        Long id = submission.getId() != null ? submission.getId() : sequence.incrementAndGet();

        Submission saved = Submission.builder()
            .id(id)
            .teamId(submission.getTeamId())
            .milestoneId(submission.getMilestoneId())
            .status(submission.getStatus())
            .currentVersion(submission.getCurrentVersion())
            .revisionDueAt(submission.getRevisionDueAt())
            .revisionProgress(submission.getRevisionProgress())
            .reopenedAt(submission.getReopenedAt())
            .reopenedBy(submission.getReopenedBy())
            .presentationOrder(submission.getPresentationOrder())
            .completedAt(submission.getCompletedAt())
            .completedBy(submission.getCompletedBy())
            .createdAt(submission.getCreatedAt())
            .updatedAt(submission.getUpdatedAt())
            .deletedAt(submission.getDeletedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Submission> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Submission> findByTeamIdAndMilestoneId(Long teamId, Long milestoneId) {
        return store.values().stream()
            .filter(submission -> submission.getTeamId().equals(teamId))
            .filter(submission -> submission.getMilestoneId().equals(milestoneId))
            .findFirst();
    }

    @Override
    public List<Submission> findAllByMilestoneId(Long milestoneId) {
        return store.values().stream()
            .filter(submission -> submission.getMilestoneId().equals(milestoneId))
            .toList();
    }
}
