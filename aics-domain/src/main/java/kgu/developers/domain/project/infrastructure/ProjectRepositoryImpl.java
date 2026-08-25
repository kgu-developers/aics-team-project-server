package kgu.developers.domain.project.infrastructure;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository {
    private final JpaProjectRepository jpaProjectRepository;
    private final EntityManager entityManager;

    @Override
    public Project save(Project project) {
        TeamJpaEntity team = entityManager.getReference(TeamJpaEntity.class, project.getTeamId());
        ProjectJpaEntity entity = ProjectJpaEntity.toEntity(project, team);
        ProjectJpaEntity savedEntity = jpaProjectRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Project> findById(Long id) {
        return jpaProjectRepository.findByIdAndDeletedAtIsNull(id)
                .map(ProjectJpaEntity::toDomain);
    }

    @Override
    public Optional<Project> findByIdForUpdate(Long id) {
        return jpaProjectRepository.findByIdForUpdate(id)
                .map(ProjectJpaEntity::toDomain);
    }

    @Override
    public List<Project> findAllById(List<Long> ids) {
        return jpaProjectRepository.findAllByIdInAndDeletedAtIsNull(ids)
                .stream()
                .map(ProjectJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Project> findAllByTeamId(Long teamId) {
        return jpaProjectRepository.findAllByTeamIdAndDeletedAtIsNull(teamId)
                .stream()
                .map(ProjectJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Project> findAllByTeamIdForUpdate(Long teamId) {
        return jpaProjectRepository.findAllByTeamIdForUpdate(teamId)
                .stream()
                .map(ProjectJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        ProjectJpaEntity project = jpaProjectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(ProjectNotFoundException::new);
        project.delete();
    }
}
