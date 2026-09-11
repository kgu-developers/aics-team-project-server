package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface PeerEvaluationSubmissionRepository {
    PeerEvaluationSubmission save(PeerEvaluationSubmission submission);

    Optional<PeerEvaluationSubmission> findByFormIdAndEvaluatorId(Long formId, String evaluatorId);

    List<PeerEvaluationSubmission> findAllByFormId(Long formId);

    List<PeerEvaluationSubmission> findAllByFormIdAndEvaluatorIdIn(Long formId, List<String> evaluatorIds);
}
