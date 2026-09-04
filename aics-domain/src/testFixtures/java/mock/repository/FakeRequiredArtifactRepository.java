package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactRepository;

public class FakeRequiredArtifactRepository implements RequiredArtifactRepository {

    private final Map<Long, RequiredArtifact> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public RequiredArtifact save(RequiredArtifact requiredArtifact) {
        Long id = requiredArtifact.getId() != null ? requiredArtifact.getId() : sequence.incrementAndGet();

        RequiredArtifact saved = RequiredArtifact.restore(
            id,
            requiredArtifact.getMilestoneId(),
            requiredArtifact.getType(),
            requiredArtifact.getLabel(),
            requiredArtifact.isRequired(),
            requiredArtifact.getAllowedExtensions(),
            requiredArtifact.getMaxFileSizeMb(),
            requiredArtifact.getCreatedAt(),
            requiredArtifact.getUpdatedAt(),
            requiredArtifact.getDeletedAt()
        );

        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<RequiredArtifact> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<RequiredArtifact> findAllByMilestoneId(Long milestoneId) {
        return store.values().stream()
            .filter(requiredArtifact -> requiredArtifact.getMilestoneId().equals(milestoneId))
            .toList();
    }
}
