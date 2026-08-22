package kgu.developers.domain.topicCandidate.domain;

import java.util.List;
import java.util.Optional;

public interface TopicCandidateRepository {
    TopicCandidate save(TopicCandidate topicCandidate);

    Optional<TopicCandidate> findById(Long id);

    List<TopicCandidate> findByTeamId(Long teamId);

    List<TopicCandidate> findByProposerUserId(String proposerUserId);

    void deleteById(Long id);
}
