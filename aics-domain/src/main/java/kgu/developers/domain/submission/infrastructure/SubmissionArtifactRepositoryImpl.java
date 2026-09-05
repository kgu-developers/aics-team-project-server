package kgu.developers.domain.submission.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.submission.domain.SubmissionArtifact;
import kgu.developers.domain.submission.domain.SubmissionArtifactRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubmissionArtifactRepositoryImpl implements SubmissionArtifactRepository {
    private final JpaSubmissionArtifactRepository jpaSubmissionArtifactRepository;

    @Override
    public List<SubmissionArtifact> saveAll(List<SubmissionArtifact> artifacts) {
        List<SubmissionArtifactJpaEntity> entities = artifacts.stream()
                .map(SubmissionArtifactJpaEntity::fromDomain)
                .toList();
        return jpaSubmissionArtifactRepository.saveAll(entities).stream()
                .map(SubmissionArtifactJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<SubmissionArtifact> findAllByVersionId(Long versionId) {
        return jpaSubmissionArtifactRepository.findAllByVersionId(versionId).stream()
                .map(SubmissionArtifactJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<SubmissionArtifact> findAllByVersionIdIn(List<Long> versionIds) {
        if (versionIds.isEmpty()) {
            return List.of();
        }
        return jpaSubmissionArtifactRepository.findAllByVersionIdIn(versionIds).stream()
                .map(SubmissionArtifactJpaEntity::toDomain)
                .toList();
    }
}
