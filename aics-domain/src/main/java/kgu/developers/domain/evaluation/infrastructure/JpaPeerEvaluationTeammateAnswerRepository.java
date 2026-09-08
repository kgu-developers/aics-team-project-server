package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPeerEvaluationTeammateAnswerRepository extends JpaRepository<PeerEvaluationTeammateAnswerJpaEntity, Long> {
    List<PeerEvaluationTeammateAnswerJpaEntity> findAllBySubmissionIdOrderById(Long submissionId);

    void deleteAllBySubmissionId(Long submissionId);
}
