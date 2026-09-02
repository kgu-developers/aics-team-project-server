package kgu.developers.domain.team.domain;

import java.util.List;
import java.util.Optional;

public interface TeamRepository {
    Team save(Team team);

    Optional<Team> findById(Long id);

    Optional<Team> findByIdForUpdate(Long id);

    List<Team> findAllById(List<Long> ids);

    List<Team> findAllBySectionId(Long sectionId);

    boolean existsBySectionIdAndNameAndIdNot(Long sectionId, String name, Long id);

    void deleteById(Long id);
}
