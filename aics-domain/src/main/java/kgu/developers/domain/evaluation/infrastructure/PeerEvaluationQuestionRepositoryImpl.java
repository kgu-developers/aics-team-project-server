package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestion;
import kgu.developers.domain.evaluation.domain.PeerEvaluationQuestionRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PeerEvaluationQuestionRepositoryImpl implements PeerEvaluationQuestionRepository {
    private final JpaPeerEvaluationQuestionRepository jpaRepository;

    @Override
    public PeerEvaluationQuestion save(PeerEvaluationQuestion question) {
        return jpaRepository.save(PeerEvaluationQuestionJpaEntity.toEntity(question)).toDomain();
    }

    @Override
    public Optional<PeerEvaluationQuestion> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(PeerEvaluationQuestionJpaEntity::toDomain);
    }

    @Override
    public List<PeerEvaluationQuestion> findAllByFormIdOrderByDisplayOrder(Long formId) {
        return jpaRepository.findAllByFormIdAndDeletedAtIsNullOrderByDisplayOrderAsc(formId)
                .stream()
                .map(PeerEvaluationQuestionJpaEntity::toDomain)
                .toList();
    }

}
