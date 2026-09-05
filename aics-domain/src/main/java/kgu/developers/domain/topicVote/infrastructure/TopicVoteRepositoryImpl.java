package kgu.developers.domain.topicVote.infrastructure;

import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import kgu.developers.domain.topicVote.exception.TopicVoteNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
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
    public List<TopicVote> findAllByCandidateIdIn(List<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return List.of();
        }
        return jpaTopicVoteRepository.findAllByCandidateIdInAndDeletedAtIsNull(candidateIds)
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
        // RETURNING 으로 결과 행을 바로 받는다. 같은 트랜잭션에서 이 행을 미리 로딩했다면 영속성 컨텍스트의
        // 옛 인스턴스가 반환되므로, 그런 호출자가 생기면 upsert 전에 clear/refresh 가 필요하다.
        return jpaTopicVoteRepository
                .upsert(topicVote.getTeamId(), topicVote.getCandidateId(), topicVote.getVoterUserId())
                .toDomain();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        softDelete(jpaTopicVoteRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(TopicVoteNotFoundException::new));
    }

    @Override
    @Transactional
    public void deleteByTeamIdAndCandidateIdAndVoterUserId(Long teamId, Long candidateId, String voterUserId) {
        softDelete(jpaTopicVoteRepository
                .findByTeamIdAndCandidateIdAndVoterUserIdAndDeletedAtIsNull(teamId, candidateId, voterUserId)
                .orElseThrow(TopicVoteNotFoundException::new));
    }

    private void softDelete(TopicVoteJpaEntity entity) {
        entity.delete();
        try {
            jpaTopicVoteRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new TopicVoteNotFoundException();   // 경쟁에서 진 취소 요청은 취소할 표가 없던 것과 같다
        }
    }
}
