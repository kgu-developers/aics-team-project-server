package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPeerEvaluationTeammateAnswerRepository extends JpaRepository<PeerEvaluationTeammateAnswerJpaEntity, Long> {
    List<PeerEvaluationTeammateAnswerJpaEntity> findAllBySubmissionIdOrderById(Long submissionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PeerEvaluationTeammateAnswerJpaEntity e WHERE e.submissionId = :submissionId")
    void deleteAllBySubmissionId(@Param("submissionId") Long submissionId);
}
