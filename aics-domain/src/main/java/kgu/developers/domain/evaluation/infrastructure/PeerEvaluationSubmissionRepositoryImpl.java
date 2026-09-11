package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
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

    @Override
    public List<PeerEvaluationSubmission> findAllByFormId(Long formId) {
        return jpaRepository.findAllByFormIdAndDeletedAtIsNull(formId).stream()
            .map(PeerEvaluationSubmissionJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<PeerEvaluationSubmission> findAllByFormIdAndEvaluatorIdIn(Long formId, List<String> evaluatorIds) {
        if (evaluatorIds == null || evaluatorIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByFormIdAndEvaluatorIdInAndDeletedAtIsNull(formId, evaluatorIds).stream()
            .map(PeerEvaluationSubmissionJpaEntity::toDomain)
            .toList();
    }
}
