package kgu.developers.domain.submission.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.submission.domain.SubmissionVersion;
import kgu.developers.domain.submission.domain.SubmissionVersionRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubmissionVersionRepositoryImpl implements SubmissionVersionRepository {
    private final JpaSubmissionVersionRepository jpaSubmissionVersionRepository;

    @Override
    public SubmissionVersion save(SubmissionVersion submissionVersion) {
        SubmissionVersionJpaEntity entity = SubmissionVersionJpaEntity.fromDomain(submissionVersion);
        return jpaSubmissionVersionRepository.save(entity).toDomain();
    }

    @Override
    public List<SubmissionVersion> findAllBySubmissionId(Long submissionId) {
        return jpaSubmissionVersionRepository.findAllBySubmissionIdOrderByVersionDesc(submissionId).stream()
                .map(SubmissionVersionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<SubmissionVersion> findBySubmissionIdAndVersion(Long submissionId, int version) {
        return jpaSubmissionVersionRepository.findBySubmissionIdAndVersion(submissionId, version)
                .map(SubmissionVersionJpaEntity::toDomain);
    }

    @Override
    public int countBySubmissionId(Long submissionId) {
        return jpaSubmissionVersionRepository.countBySubmissionId(submissionId);
    }
}
