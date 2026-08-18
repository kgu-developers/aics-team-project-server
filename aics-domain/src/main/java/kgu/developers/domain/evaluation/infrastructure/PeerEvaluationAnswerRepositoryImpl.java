package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.evaluation.domain.PeerEvaluationAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationAnswerRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PeerEvaluationAnswerRepositoryImpl implements PeerEvaluationAnswerRepository {
    private final JpaPeerEvaluationAnswerRepository jpaRepository;

    @Override
    public PeerEvaluationAnswer save(PeerEvaluationAnswer answer) {
        return jpaRepository.save(PeerEvaluationAnswerJpaEntity.toEntity(answer)).toDomain();
    }

    @Override
    public Optional<PeerEvaluationAnswer> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(PeerEvaluationAnswerJpaEntity::toDomain);
    }

    @Override
    public List<PeerEvaluationAnswer> findAllByResponseId(Long responseId) {
        return jpaRepository.findAllByResponseIdAndDeletedAtIsNull(responseId)
                .stream()
                .map(PeerEvaluationAnswerJpaEntity::toDomain)
                .toList();
    }

}
