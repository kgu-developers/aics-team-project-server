package mock.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;

public class FakeSubmissionVersionRepository implements SubmissionVersionRepository {

    private final Map<Long, SubmissionVersion> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public SubmissionVersion save(SubmissionVersion submissionVersion) {
        Long id = submissionVersion.getId() != null ? submissionVersion.getId() : sequence.incrementAndGet();

        SubmissionVersion saved = SubmissionVersion.builder()
            .id(id)
            .submissionId(submissionVersion.getSubmissionId())
            .version(submissionVersion.getVersion())
            .description(submissionVersion.getDescription())
            .changeNote(submissionVersion.getChangeNote())
            .submittedBy(submissionVersion.getSubmittedBy())
            .submittedAt(submissionVersion.getSubmittedAt())
            .late(submissionVersion.isLate())
            .createdAt(submissionVersion.getCreatedAt() != null ? submissionVersion.getCreatedAt() : LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deletedAt(submissionVersion.getDeletedAt())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public List<SubmissionVersion> findAllBySubmissionId(Long submissionId) {
        return store.values().stream()
            .filter(version -> version.getSubmissionId().equals(submissionId))
            .sorted(Comparator.comparingInt(SubmissionVersion::getVersion).reversed())
            .toList();
    }

    @Override
    public Optional<SubmissionVersion> findBySubmissionIdAndVersion(Long submissionId, int version) {
        return store.values().stream()
            .filter(v -> v.getSubmissionId().equals(submissionId))
            .filter(v -> v.getVersion() == version)
            .findFirst();
    }

    @Override
    public int countBySubmissionId(Long submissionId) {
        return (int) store.values().stream()
            .filter(version -> version.getSubmissionId().equals(submissionId))
            .count();
    }
}
