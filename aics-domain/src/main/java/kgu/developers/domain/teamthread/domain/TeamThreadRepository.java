package kgu.developers.domain.teamthread.domain;

import java.util.Optional;

public interface TeamThreadRepository {

    TeamThread save(TeamThread teamThread);

    Optional<TeamThread> findById(Long id);

    Optional<TeamThread> findByTeamId(Long teamId);
}
