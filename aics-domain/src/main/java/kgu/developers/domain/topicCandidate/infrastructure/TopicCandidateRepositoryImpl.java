package kgu.developers.domain.topicCandidate.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
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
    public Optional<TopicCandidate> findByIdForUpdate(Long id) {
        return jpaTopicCandidateRepository.findById(id)
                .map(this::lockTeamAndRefresh)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(TopicCandidateJpaEntity::toDomain);
    }

    @Override
    public void lockTeamForUpdate(Long teamId) {
        entityManager.find(TeamJpaEntity.class, teamId, PESSIMISTIC_WRITE);
    }

    private TopicCandidateJpaEntity lockTeamAndRefresh(TopicCandidateJpaEntity entity) {
        Long lockedTeamId = entity.getTeamId();
        lockTeamForUpdate(lockedTeamId);
        entityManager.refresh(entity);
        if (!lockedTeamId.equals(entity.getTeamId())) {
            throw new OptimisticLockingFailureException(
                    "주제 후보 %d의 팀이 잠금 획득 전에 변경되었습니다".formatted(entity.getId()));
        }
        return entity;
    }

    @Override
    public Optional<TopicCandidate> findIncludingDeletedByTeamIdAndTitleForUpdate(Long teamId, String title) {
        lockTeamForUpdate(teamId);
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
}
