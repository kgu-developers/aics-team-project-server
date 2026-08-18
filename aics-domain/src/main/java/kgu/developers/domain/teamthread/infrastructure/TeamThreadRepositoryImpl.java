package kgu.developers.domain.teamthread.infrastructure;

import java.util.Optional;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.teamthread.domain.TeamThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TeamThreadRepositoryImpl implements TeamThreadRepository {

    private final JpaTeamThreadRepository jpaTeamThreadRepository;

    @Override
    public TeamThread save(TeamThread teamThread) {
        return jpaTeamThreadRepository.save(TeamThreadJpaEntity.toEntity(teamThread)).toDomain();
    }

    @Override
    public Optional<TeamThread> findById(Long id) {
        return jpaTeamThreadRepository.findById(id).map(TeamThreadJpaEntity::toDomain);
    }

    @Override
    public Optional<TeamThread> findByTeamId(Long teamId) {
        return jpaTeamThreadRepository.findByTeamId(teamId).map(TeamThreadJpaEntity::toDomain);
    }
}
