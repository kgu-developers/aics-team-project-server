package kgu.developers.domain.topicVote.infrastructure;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTopicVoteRepository extends JpaRepository<TopicVoteJpaEntity, Long> {
    Optional<TopicVoteJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TopicVoteJpaEntity> findAllByCandidateIdAndDeletedAtIsNull(Long candidateId);

    List<TopicVoteJpaEntity> findAllByCandidateIdInAndDeletedAtIsNull(List<Long> candidateIds);

    Optional<TopicVoteJpaEntity> findByTeamIdAndVoterUserIdAndDeletedAtIsNull(Long teamId, String voterUserId);

    Optional<TopicVoteJpaEntity> findByCandidateIdAndVoterUserIdAndDeletedAtIsNull(Long candidateId, String voterUserId);

    Optional<TopicVoteJpaEntity> findByTeamIdAndVoterUserId(Long teamId, String voterUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tv FROM TopicVoteJpaEntity tv WHERE tv.teamId = :teamId AND tv.voterUserId = :voterUserId")
    Optional<TopicVoteJpaEntity> findByTeamIdAndVoterUserIdWithLock(@Param("teamId") Long teamId, @Param("voterUserId") String voterUserId);
}
