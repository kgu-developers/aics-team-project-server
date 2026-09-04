package kgu.developers.domain.submission.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubmissionMemberConfirmationRepository extends JpaRepository<SubmissionMemberConfirmationJpaEntity, Long> {
    List<SubmissionMemberConfirmationJpaEntity> findAllBySubmissionId(Long submissionId);

    Optional<SubmissionMemberConfirmationJpaEntity> findBySubmissionIdAndUserId(Long submissionId, String userId);
}
