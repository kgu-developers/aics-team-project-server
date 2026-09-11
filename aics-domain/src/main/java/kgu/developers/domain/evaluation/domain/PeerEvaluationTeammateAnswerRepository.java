package kgu.developers.domain.evaluation.domain;

import java.util.List;

public interface PeerEvaluationTeammateAnswerRepository {
    List<PeerEvaluationTeammateAnswer> saveAll(List<PeerEvaluationTeammateAnswer> answers);

    List<PeerEvaluationTeammateAnswer> findAllBySubmissionId(Long submissionId);

    List<PeerEvaluationTeammateAnswer> findAllBySubmissionIdIn(List<Long> submissionIds);

    void deleteAllBySubmissionId(Long submissionId);
}
