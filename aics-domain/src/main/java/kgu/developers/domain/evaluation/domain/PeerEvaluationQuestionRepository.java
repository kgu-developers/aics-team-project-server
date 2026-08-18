package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface PeerEvaluationQuestionRepository {
    PeerEvaluationQuestion save(PeerEvaluationQuestion question);

    Optional<PeerEvaluationQuestion> findById(Long id);

    List<PeerEvaluationQuestion> findAllByFormIdOrderByDisplayOrder(Long formId);
}
