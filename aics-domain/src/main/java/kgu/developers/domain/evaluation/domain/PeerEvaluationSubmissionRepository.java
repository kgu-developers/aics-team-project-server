package kgu.developers.domain.evaluation.domain;

import java.util.Optional;

public interface PeerEvaluationSubmissionRepository {
    PeerEvaluationSubmission save(PeerEvaluationSubmission submission);

    Optional<PeerEvaluationSubmission> findByFormIdAndEvaluatorId(Long formId, String evaluatorId);
}
