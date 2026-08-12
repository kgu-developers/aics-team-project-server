package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPeerEvaluationQuestionRepository extends JpaRepository<PeerEvaluationQuestionJpaEntity, Long> {
    Optional<PeerEvaluationQuestionJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<PeerEvaluationQuestionJpaEntity> findAllByFormIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long formId);
}
