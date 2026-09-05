package mock.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;

public class FakeSubmissionArtifactRepository implements SubmissionArtifactRepository {

    private final Map<Long, SubmissionArtifact> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public List<SubmissionArtifact> saveAll(List<SubmissionArtifact> artifacts) {
        return artifacts.stream().map(this::save).toList();
    }

    private SubmissionArtifact save(SubmissionArtifact artifact) {
        Long id = artifact.getId() != null ? artifact.getId() : sequence.incrementAndGet();

        SubmissionArtifact saved = SubmissionArtifact.builder()
            .id(id)
            .versionId(artifact.getVersionId())
            .requiredArtifactId(artifact.getRequiredArtifactId())
            .type(artifact.getType())
            .fileId(artifact.getFileId())
            .url(artifact.getUrl())
            .content(artifact.getContent())
            .build();

        store.put(id, saved);
        return saved;
    }

    @Override
    public List<SubmissionArtifact> findAllByVersionId(Long versionId) {
        return store.values().stream()
            .filter(artifact -> artifact.getVersionId().equals(versionId))
            .toList();
    }

    @Override
    public List<SubmissionArtifact> findAllByVersionIdIn(List<Long> versionIds) {
        return store.values().stream()
            .filter(artifact -> versionIds.contains(artifact.getVersionId()))
            .toList();
    }
}
