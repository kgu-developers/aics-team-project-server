package kgu.developers.domain.topicVote.domain;

import java.util.List;
import java.util.Optional;

public interface TopicVoteRepository {
    TopicVote save(TopicVote topicVote);

    Optional<TopicVote> findById(Long id);

    List<TopicVote> findAllByCandidateId(Long candidateId);

    List<TopicVote> findAllByCandidateIdIn(List<Long> candidateIds);

    Optional<TopicVote> findByTeamIdAndVoterUserId(Long teamId, String voterUserId);

    Optional<TopicVote> findByCandidateIdAndVoterUserId(Long candidateId, String voterUserId);

    Optional<TopicVote> findByTeamIdAndVoterUserIdIncludingDeleted(Long teamId, String voterUserId);

    Optional<TopicVote> findByTeamIdAndVoterUserIdWithLock(Long teamId, String voterUserId);

    Optional<TopicVote> findByCandidateIdAndVoterUserIdWithLock(Long candidateId, String voterUserId);

    void deleteById(Long id);

    void deleteByTeamIdAndVoterUserId(Long teamId, String voterUserId);

    void deleteByCandidateIdAndVoterUserId(Long candidateId, String voterUserId);
}
