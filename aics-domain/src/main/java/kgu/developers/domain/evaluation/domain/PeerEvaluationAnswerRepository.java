package kgu.developers.domain.evaluation.domain;

import java.util.List;
import java.util.Optional;

public interface PeerEvaluationAnswerRepository {
    PeerEvaluationAnswer save(PeerEvaluationAnswer answer);

    Optional<PeerEvaluationAnswer> findById(Long id);

    List<PeerEvaluationAnswer> findAllByResponseId(Long responseId);
}
