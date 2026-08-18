package kgu.developers.domain.evaluation.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPeerEvaluationFormRepository extends JpaRepository<PeerEvaluationFormJpaEntity, Long> {
    Optional<PeerEvaluationFormJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
