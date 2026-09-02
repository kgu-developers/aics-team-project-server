package kgu.developers.domain.topicVote.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTopicVoteRepository extends JpaRepository<TopicVoteJpaEntity, Long> {
    Optional<TopicVoteJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TopicVoteJpaEntity> findAllByCandidateIdAndDeletedAtIsNull(Long candidateId);

    Optional<TopicVoteJpaEntity> findByTeamIdAndVoterUserIdAndDeletedAtIsNull(Long teamId, String voterUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO topic_vote (team_id, candidate_id, voter_user_id, version, created_at, updated_at)
            VALUES (:teamId, :candidateId, :voterUserId, 0, now(), now())
            ON CONFLICT (team_id, voter_user_id) DO UPDATE
            SET candidate_id = EXCLUDED.candidate_id, deleted_at = NULL,
                version = topic_vote.version + 1, updated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("teamId") Long teamId,
                @Param("candidateId") Long candidateId,
                @Param("voterUserId") String voterUserId);
}
