package kgu.developers.domain.topicCandidate.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import lombok.RequiredArgsConstructor;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Repository
@RequiredArgsConstructor
public class TopicCandidateRepositoryImpl implements TopicCandidateRepository {
    private final JpaTopicCandidateRepository jpaTopicCandidateRepository;
    private final EntityManager entityManager;

    @Override
    public TopicCandidate save(TopicCandidate topicCandidate) {
        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.toEntity(topicCandidate);
        return jpaTopicCandidateRepository.save(entity).toDomain();
    }

    @Override
    public Optional<TopicCandidate> findById(Long id) {
        Optional<TopicCandidateJpaEntity> optionalEntity = jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(id);
        return optionalEntity.map(TopicCandidateJpaEntity::toDomain);
    }

    @Override
    public Optional<TopicCandidate> findActiveByTeamIdAndTitleForUpdate(Long teamId, String title) {
        entityManager.find(TeamJpaEntity.class, teamId, PESSIMISTIC_WRITE);
        return jpaTopicCandidateRepository.findByTeamIdAndTitleAndDeletedAtIsNull(teamId, title)
                .map(TopicCandidateJpaEntity::toDomain);
    }

    @Override
    public Optional<TopicCandidate> findIncludingDeletedByTeamIdAndTitle(Long teamId, String title) {
        return jpaTopicCandidateRepository.findByTeamIdAndTitle(teamId, title)
                .map(TopicCandidateJpaEntity::toDomain);
    }

    @Override
    public List<TopicCandidate> findByTeamId(Long teamId) {
        List<TopicCandidateJpaEntity> entities = jpaTopicCandidateRepository.findByTeamIdAndDeletedAtIsNull(teamId);
        return entities.stream()
                .map(TopicCandidateJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<TopicCandidate> findByProposerUserId(String proposerUserId) {
        List<TopicCandidateJpaEntity> entities = jpaTopicCandidateRepository.findByProposerUserIdAndDeletedAtIsNull(proposerUserId);
        return entities.stream()
                .map(TopicCandidateJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TopicCandidateJpaEntity entity = jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(TopicCandidateNotFoundException::new);
        entity.delete();
        jpaTopicCandidateRepository.save(entity);
    }
}
