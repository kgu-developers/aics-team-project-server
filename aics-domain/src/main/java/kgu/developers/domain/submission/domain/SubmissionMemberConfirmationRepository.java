package kgu.developers.domain.submission.domain;

import java.util.List;
import java.util.Optional;

public interface SubmissionMemberConfirmationRepository {
    SubmissionMemberConfirmation save(SubmissionMemberConfirmation confirmation);

    List<SubmissionMemberConfirmation> findAllBySubmissionId(Long submissionId);

    Optional<SubmissionMemberConfirmation> findBySubmissionIdAndUserId(Long submissionId, String userId);
}
