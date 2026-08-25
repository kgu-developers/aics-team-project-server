package kgu.developers.domain.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);

    Optional<Project> findById(Long id);

    List<Project> findAllById(List<Long> ids);

    List<Project> findAllByTeamId(Long teamId);

    void deleteById(Long id);
}