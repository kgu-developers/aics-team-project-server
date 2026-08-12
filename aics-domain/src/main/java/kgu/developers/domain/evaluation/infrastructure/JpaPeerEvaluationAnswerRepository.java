package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPeerEvaluationAnswerRepository extends JpaRepository<PeerEvaluationAnswerJpaEntity, Long> {
    Optional<PeerEvaluationAnswerJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<PeerEvaluationAnswerJpaEntity> findAllByResponseIdAndDeletedAtIsNull(Long responseId);
}
