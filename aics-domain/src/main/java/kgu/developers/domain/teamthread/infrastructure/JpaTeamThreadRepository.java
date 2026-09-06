package kgu.developers.domain.teamthread.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTeamThreadRepository extends JpaRepository<TeamThreadJpaEntity, Long> {

    Optional<TeamThreadJpaEntity> findByTeamId(Long teamId);

    List<TeamThreadJpaEntity> findAllByTeamIdIn(List<Long> teamIds);
}
