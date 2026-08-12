package kgu.developers.domain.evaluation.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.evaluation.domain.PeerEvaluationResponse;
import kgu.developers.domain.evaluation.domain.PeerEvaluationResponseRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PeerEvaluationResponseRepositoryImpl implements PeerEvaluationResponseRepository {
    private final JpaPeerEvaluationResponseRepository jpaRepository;

    @Override
    public PeerEvaluationResponse save(PeerEvaluationResponse response) {
        return jpaRepository.save(PeerEvaluationResponseJpaEntity.toEntity(response)).toDomain();
    }

    @Override
    public Optional<PeerEvaluationResponse> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(PeerEvaluationResponseJpaEntity::toDomain);
    }

    @Override
    public Optional<PeerEvaluationResponse> findByFormIdAndEvaluatorIdAndTargetId(Long formId, String evaluatorId, String targetId) {
        return jpaRepository.findByFormIdAndEvaluatorIdAndTargetIdAndDeletedAtIsNull(
                        formId,
                        normalizeStudentNumber(evaluatorId),
                        normalizeStudentNumber(targetId)
                )
                .map(PeerEvaluationResponseJpaEntity::toDomain);
    }

    private static String normalizeStudentNumber(String studentNumber) {
        return studentNumber == null ? null : studentNumber.trim();
    }

}
