package kgu.developers.domain.topicCandidate.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.DuplicateTopicCandidateTitleException;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TopicCandidateRepositoryImpl implements TopicCandidateRepository {
    private final JpaTopicCandidateRepository jpaTopicCandidateRepository;

    @Override
    public TopicCandidate save(TopicCandidate topicCandidate) {
        jpaTopicCandidateRepository
                .findByTeamIdAndTitleAndDeletedAtIsNull(topicCandidate.getTeamId(), topicCandidate.getTitle())
                .filter(found -> !found.getId().equals(topicCandidate.getId()))
                .ifPresent(found -> {
                    throw new DuplicateTopicCandidateTitleException();
                });

        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.toEntity(topicCandidate);
        return jpaTopicCandidateRepository.save(entity).toDomain();
    }

    @Override
    public Optional<TopicCandidate> findById(Long id) {
        Optional<TopicCandidateJpaEntity> optionalEntity = jpaTopicCandidateRepository.findByIdAndDeletedAtIsNull(id);
        return optionalEntity.map(TopicCandidateJpaEntity::toDomain);
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
    }
}
