package kgu.developers.domain.evaluation.infrastructure;

import java.util.List;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswer;
import kgu.developers.domain.evaluation.domain.PeerEvaluationTeammateAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PeerEvaluationTeammateAnswerRepositoryImpl implements PeerEvaluationTeammateAnswerRepository {
    private final JpaPeerEvaluationTeammateAnswerRepository jpaRepository;

    @Override
    public List<PeerEvaluationTeammateAnswer> saveAll(List<PeerEvaluationTeammateAnswer> answers) {
        return jpaRepository.saveAll(answers.stream().map(PeerEvaluationTeammateAnswerJpaEntity::from).toList())
            .stream().map(PeerEvaluationTeammateAnswerJpaEntity::toDomain).toList();
    }

    @Override
    public List<PeerEvaluationTeammateAnswer> findAllBySubmissionId(Long submissionId) {
        return jpaRepository.findAllBySubmissionIdOrderById(submissionId).stream()
            .map(PeerEvaluationTeammateAnswerJpaEntity::toDomain).toList();
    }

    @Override
    public List<PeerEvaluationTeammateAnswer> findAllBySubmissionIdIn(List<Long> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllBySubmissionIdInOrderById(submissionIds).stream()
            .map(PeerEvaluationTeammateAnswerJpaEntity::toDomain).toList();
    }

    @Override
    public void deleteAllBySubmissionId(Long submissionId) {
        jpaRepository.deleteAllBySubmissionId(submissionId);
        jpaRepository.flush();
    }
}
