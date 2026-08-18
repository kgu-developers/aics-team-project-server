package kgu.developers.domain.evaluation.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import kgu.developers.domain.evaluation.domain.PeerEvaluationFormRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PeerEvaluationFormRepositoryImpl implements PeerEvaluationFormRepository {
    private final JpaPeerEvaluationFormRepository jpaRepository;

    @Override
    public PeerEvaluationForm save(PeerEvaluationForm form) {
        return jpaRepository.save(PeerEvaluationFormJpaEntity.toEntity(form)).toDomain();
    }

    @Override
    public Optional<PeerEvaluationForm> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(PeerEvaluationFormJpaEntity::toDomain);
    }

}
