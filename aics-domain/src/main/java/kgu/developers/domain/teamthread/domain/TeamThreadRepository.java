package kgu.developers.domain.teamthread.domain;

import java.util.List;
import java.util.Optional;

public interface TeamThreadRepository {

    TeamThread save(TeamThread teamThread);

    Optional<TeamThread> findById(Long id);

    Optional<TeamThread> findByTeamId(Long teamId);

    List<TeamThread> findAllByTeamIdIn(List<Long> teamIds);
}
