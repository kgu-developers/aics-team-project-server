package kgu.developers.domain.topicVote.domain;

import java.util.List;
import java.util.Optional;

public interface TopicVoteRepository {
    /** 신규 저장 전용. 투표/재투표에 쓰면 (team_id, voter_user_id) 유니크 제약에 걸린다 -> upsert 를 쓸 것. */
    TopicVote save(TopicVote topicVote);

    Optional<TopicVote> findById(Long id);

    List<TopicVote> findAllByCandidateId(Long candidateId);

    Optional<TopicVote> findByTeamIdAndVoterUserId(Long teamId, String voterUserId);

    TopicVote upsert(TopicVote topicVote);

    void deleteById(Long id);

    void deleteByTeamIdAndCandidateIdAndVoterUserId(Long teamId, Long candidateId, String voterUserId);
}
