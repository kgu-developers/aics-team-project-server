package kgu.developers.domain.topicVote.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTopicVoteRepository extends JpaRepository<TopicVoteJpaEntity, Long> {
    Optional<TopicVoteJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TopicVoteJpaEntity> findAllByCandidateIdAndDeletedAtIsNull(Long candidateId);

    List<TopicVoteJpaEntity> findAllByCandidateIdInAndDeletedAtIsNull(List<Long> candidateIds);

    Optional<TopicVoteJpaEntity> findByCandidateIdAndVoterUserIdAndDeletedAtIsNull(Long candidateId, String voterUserId);
}
