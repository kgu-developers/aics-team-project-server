package kgu.developers.domain.evaluation.domain;

import java.util.List;

public interface PeerEvaluationTeammateAnswerRepository {
    List<PeerEvaluationTeammateAnswer> saveAll(List<PeerEvaluationTeammateAnswer> answers);

    List<PeerEvaluationTeammateAnswer> findAllBySubmissionId(Long submissionId);

    void deleteAllBySubmissionId(Long submissionId);
}
