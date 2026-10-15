package kgu.developers.domain.evaluation.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPeerEvaluationResponseRepository extends JpaRepository<PeerEvaluationResponseJpaEntity, Long> {
    Optional<PeerEvaluationResponseJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<PeerEvaluationResponseJpaEntity> findByFormIdAndEvaluatorIdAndTargetIdAndDeletedAtIsNull(
            Long formId,
            String evaluatorId,
            String targetId
    );
}
