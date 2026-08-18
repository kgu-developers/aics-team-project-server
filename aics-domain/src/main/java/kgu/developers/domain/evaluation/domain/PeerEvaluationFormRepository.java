package kgu.developers.domain.evaluation.domain;

import java.util.Optional;

public interface PeerEvaluationFormRepository {
    PeerEvaluationForm save(PeerEvaluationForm form);

    Optional<PeerEvaluationForm> findById(Long id);
}
