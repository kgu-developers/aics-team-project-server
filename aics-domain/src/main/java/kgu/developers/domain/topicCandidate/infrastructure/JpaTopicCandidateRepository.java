package kgu.developers.domain.topicCandidate.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTopicCandidateRepository extends JpaRepository<TopicCandidateJpaEntity, Long> {
    Optional<TopicCandidateJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TopicCandidateJpaEntity> findByTeamIdAndDeletedAtIsNull(Long teamId);

    List<TopicCandidateJpaEntity> findByProposerUserIdAndDeletedAtIsNull(String proposerUserId);

    Optional<TopicCandidateJpaEntity> findByTeamIdAndTitle(Long teamId, String title);
}
