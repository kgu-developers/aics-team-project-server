package kgu.developers.domain.topicVote.domain;

import java.util.List;
import java.util.Optional;

public interface TopicVoteRepository {
    TopicVote save(TopicVote topicVote);

    Optional<TopicVote> findById(Long id);

    List<TopicVote> findAllByCandidateId(Long candidateId);

    Optional<TopicVote> findByTeamIdAndVoterUserId(Long teamId, String voterUserId);

    TopicVote upsert(TopicVote topicVote);

    void deleteById(Long id);

    void deleteByTeamIdAndVoterUserId(Long teamId, String voterUserId);
}
