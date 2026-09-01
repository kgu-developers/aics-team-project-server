package kgu.developers.domain.topicVote.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTopicVoteRepository extends JpaRepository<TopicVoteJpaEntity, Long> {
    Optional<TopicVoteJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TopicVoteJpaEntity> findAllByCandidateIdAndDeletedAtIsNull(Long candidateId);

    Optional<TopicVoteJpaEntity> findByTeamIdAndVoterUserIdAndDeletedAtIsNull(Long teamId, String voterUserId);

    Optional<TopicVoteJpaEntity> findByTeamIdAndVoterUserId(Long teamId, String voterUserId);
}
