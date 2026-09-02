package kgu.developers.domain.project.infrastructure;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectAlreadyExistsException;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.project.exception.ProjectVersionConflictException;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepository {
    private final JpaProjectRepository jpaProjectRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Project save(Project project) {
        try {
            TeamJpaEntity team = entityManager.find(
                    TeamJpaEntity.class, project.getTeamId(), PESSIMISTIC_WRITE);
            if (team == null || team.getDeletedAt() != null) {
                throw new TeamNotFoundException();
            }
            if (project.getId() == null
                    && !jpaProjectRepository.findAllByTeamIdAndDeletedAtIsNull(project.getTeamId()).isEmpty()) {
                throw new ProjectAlreadyExistsException();
            }
            ProjectJpaEntity entity = ProjectJpaEntity.toEntity(project, team);
            ProjectJpaEntity savedEntity = jpaProjectRepository.saveAndFlush(entity);
            return savedEntity.toDomain();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectVersionConflictException();
        }
    }

    @Override
    public Optional<Project> findById(Long id) {
        return jpaProjectRepository.findByIdAndDeletedAtIsNull(id)
                .map(ProjectJpaEntity::toDomain);
    }

    @Override
    public List<Project> findAllById(List<Long> ids) {
        return jpaProjectRepository.findAllByIdInAndDeletedAtIsNullOrderByIdAsc(ids)
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
    public Optional<Project> findIncludingDeletedByTeamId(Long teamId) {
        return jpaProjectRepository.findByTeamId(teamId)
                .map(ProjectJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        try {
            ProjectJpaEntity project = jpaProjectRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(ProjectNotFoundException::new);
            project.delete();
            jpaProjectRepository.saveAndFlush(project);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectVersionConflictException();
        }
    }
}
