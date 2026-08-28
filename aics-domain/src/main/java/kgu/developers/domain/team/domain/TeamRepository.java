package kgu.developers.domain.team.domain;

import java.util.List;
import java.util.Optional;

public interface TeamRepository {
    Team save(Team team);

    Optional<Team> findById(Long id);

    List<Team> findAllById(List<Long> ids);

    List<Team> findAllBySectionId(Long sectionId);

    List<Team> findAllBySectionIdIn(List<Long> sectionIds);

    void deleteById(Long id);
}
