package kgu.developers.domain.submission.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubmissionVersionRepository extends JpaRepository<SubmissionVersionJpaEntity, Long> {
    List<SubmissionVersionJpaEntity> findAllBySubmissionIdAndDeletedAtIsNullOrderByVersionDesc(Long submissionId);

    Optional<SubmissionVersionJpaEntity> findBySubmissionIdAndVersionAndDeletedAtIsNull(Long submissionId, int version);

    int countBySubmissionIdAndDeletedAtIsNull(Long submissionId);
}
