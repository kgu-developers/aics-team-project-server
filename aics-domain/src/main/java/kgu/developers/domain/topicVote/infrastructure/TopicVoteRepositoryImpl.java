package kgu.developers.domain.topicVote.infrastructure;

import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import kgu.developers.domain.topicVote.exception.TopicVoteNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TopicVoteRepositoryImpl implements TopicVoteRepository {
    private final JpaTopicVoteRepository jpaTopicVoteRepository;

    @Override
    public TopicVote save(TopicVote topicVote) {
        TopicVoteJpaEntity entity = TopicVoteJpaEntity.toEntity(topicVote);
        TopicVoteJpaEntity savedEntity = jpaTopicVoteRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<TopicVote> findById(Long id) {
        return jpaTopicVoteRepository.findByIdAndDeletedAtIsNull(id)
                .map(TopicVoteJpaEntity::toDomain);
    }

    @Override
    public List<TopicVote> findAllByCandidateId(Long candidateId) {
        return jpaTopicVoteRepository.findAllByCandidateIdAndDeletedAtIsNull(candidateId)
                .stream()
                .map(TopicVoteJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TopicVote> findByTeamIdAndVoterUserId(Long teamId, String voterUserId) {
        return jpaTopicVoteRepository.findByTeamIdAndVoterUserIdAndDeletedAtIsNull(teamId, voterUserId)
                .map(TopicVoteJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public TopicVote upsert(TopicVote topicVote) {
        jpaTopicVoteRepository.upsert(topicVote.getTeamId(), topicVote.getCandidateId(), topicVote.getVoterUserId());
        return jpaTopicVoteRepository
                .findByTeamIdAndVoterUserIdAndDeletedAtIsNull(topicVote.getTeamId(), topicVote.getVoterUserId())
                .map(TopicVoteJpaEntity::toDomain)
                .orElseThrow(TopicVoteNotFoundException::new);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TopicVoteJpaEntity entity = jpaTopicVoteRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(TopicVoteNotFoundException::new);
        entity.delete();
    }

    @Override
    @Transactional
    public void deleteByTeamIdAndVoterUserId(Long teamId, String voterUserId) {
        TopicVoteJpaEntity entity = jpaTopicVoteRepository.findByTeamIdAndVoterUserIdAndDeletedAtIsNull(teamId, voterUserId)
                .orElseThrow(TopicVoteNotFoundException::new);
        entity.delete();
    }
}
