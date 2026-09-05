package kgu.developers.domain.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    Project save(Project project);

    Project reactivate(Long projectId, Project newProject);

    /**
     * 팀 행을 잠근다. 팀 단위 검증(팀장 여부, 팀원 동의 현황)을 팀 변경과 직렬화할 때 쓴다.
     */
    void lockTeam(Long teamId);

    Optional<Project> findById(Long id);

    Optional<Project> findByIdForUpdate(Long id);

    List<Project> findAllByTeamId(Long teamId);

    Optional<Project> findIncludingDeletedByTeamId(Long teamId);

    void deleteById(Long id);
}
