package kgu.developers.domain.evaluation.infrastructure;

import java.util.Optional;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmission;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PeerEvaluationSubmissionRepositoryImpl implements PeerEvaluationSubmissionRepository {
    private final JpaPeerEvaluationSubmissionRepository jpaRepository;

    @Override
    public PeerEvaluationSubmission save(PeerEvaluationSubmission submission) {
        return jpaRepository.save(PeerEvaluationSubmissionJpaEntity.from(submission)).toDomain();
    }

    @Override
    public Optional<PeerEvaluationSubmission> findByFormIdAndEvaluatorId(Long formId, String evaluatorId) {
        return jpaRepository.findByFormIdAndEvaluatorIdAndDeletedAtIsNull(formId, evaluatorId.trim())
            .map(PeerEvaluationSubmissionJpaEntity::toDomain);
    }
}
