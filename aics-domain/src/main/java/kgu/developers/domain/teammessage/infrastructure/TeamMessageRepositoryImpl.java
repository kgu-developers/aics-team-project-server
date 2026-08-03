package kgu.developers.domain.teammessage.infrastructure;

import java.util.Optional;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teammessage.domain.TeamMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamMessageRepositoryImpl implements TeamMessageRepository {

    private final JpaTeamMessageRepository jpaTeamMessageRepository;

    @Override
    public TeamMessage save(TeamMessage teamMessage) {
        return jpaTeamMessageRepository.save(TeamMessageJpaEntity.toEntity(teamMessage)).toDomain();
    }

    @Override
    public Optional<TeamMessage> findById(Long id) {
        return jpaTeamMessageRepository.findById(id).map(TeamMessageJpaEntity::toDomain);
    }

    @Override
    public Page<TeamMessage> findByThreadId(Long threadId, Pageable pageable) {
        return jpaTeamMessageRepository.findByThreadId(threadId, pageable).map(TeamMessageJpaEntity::toDomain);
    }

    @Override
    public Page<TeamMessage> findByThreadIdAndRelatedType(Long threadId, TeamMessageRelatedType relatedType, Pageable pageable) {
        return jpaTeamMessageRepository.findByThreadIdAndRelatedType(threadId, relatedType, pageable)
            .map(TeamMessageJpaEntity::toDomain);
    }

    @Override
    public long countByThreadIdAndIsReadFalse(Long threadId) {
        return jpaTeamMessageRepository.countByThreadIdAndIsReadFalse(threadId);
    }
}
