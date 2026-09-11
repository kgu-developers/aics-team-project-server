package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPeerEvaluationSubmissionRepository extends JpaRepository<PeerEvaluationSubmissionJpaEntity, Long> {
    Optional<PeerEvaluationSubmissionJpaEntity> findByFormIdAndEvaluatorIdAndDeletedAtIsNull(Long formId, String evaluatorId);

    List<PeerEvaluationSubmissionJpaEntity> findAllByFormIdAndDeletedAtIsNull(Long formId);

    List<PeerEvaluationSubmissionJpaEntity> findAllByFormIdAndEvaluatorIdInAndDeletedAtIsNull(Long formId, List<String> evaluatorIds);
}
