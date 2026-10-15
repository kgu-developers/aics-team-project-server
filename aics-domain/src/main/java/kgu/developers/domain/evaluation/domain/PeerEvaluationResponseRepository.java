package kgu.developers.domain.evaluation.domain;

import java.util.Optional;

public interface PeerEvaluationResponseRepository {
    PeerEvaluationResponse save(PeerEvaluationResponse response);

    Optional<PeerEvaluationResponse> findById(Long id);

    Optional<PeerEvaluationResponse> findByFormIdAndEvaluatorIdAndTargetId(Long formId, String evaluatorId, String targetId);
}
