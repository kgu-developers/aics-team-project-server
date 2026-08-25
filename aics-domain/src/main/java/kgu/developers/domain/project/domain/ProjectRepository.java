package kgu.developers.domain.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);

    Optional<Project> findById(Long id);

    Optional<Project> findByIdForUpdate(Long id);

    List<Project> findAllById(List<Long> ids);

    List<Project> findAllByTeamId(Long teamId);

    List<Project> findAllByTeamIdForUpdate(Long teamId);

    void deleteById(Long id);
}
