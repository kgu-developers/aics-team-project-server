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

            Project existing = findIncludingDeletedByTeamId(project.getTeamId()).orElse(null);
            if (existing != null) {
                if (existing.getDeletedAt() == null) {
                    if (!existing.getId().equals(project.getId())) {
                        throw new ProjectAlreadyExistsException();
                    }
                } else {
                    if (project.getId() != null && project.getId().equals(existing.getId())) {
                        throw new ProjectVersionConflictException();
                    }
                    throw new ProjectVersionConflictException();
                }
            }

            ProjectJpaEntity entity = ProjectJpaEntity.toEntity(project, team);
            ProjectJpaEntity savedEntity = jpaProjectRepository.saveAndFlush(entity);
            return savedEntity.toDomain();
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectVersionConflictException();
        }
    }

    @Override
    @Transactional
    public Project reactivate(Long projectId, Project newProject) {
        try {
            TeamJpaEntity team = entityManager.find(
                    TeamJpaEntity.class, newProject.getTeamId(), PESSIMISTIC_WRITE);
            if (team == null || team.getDeletedAt() != null) {
                throw new TeamNotFoundException();
            }

            Project existing = findIncludingDeletedByTeamId(newProject.getTeamId())
                    .orElseThrow(ProjectNotFoundException::new);

            if (existing.getDeletedAt() == null) {
                throw new IllegalStateException("이미 활성화된 프로젝트는 재활성화할 수 없습니다.");
            }

            if (!existing.getId().equals(projectId)) {
                throw new ProjectNotFoundException();
            }

            existing.reactivate(newProject.getTitle(), newProject.getDescription(), newProject.getGoal(),
                    newProject.getRepositoryUrl(), newProject.getExternalLinks(), newProject.getApprovalStatus(), newProject.getMeetingStyle());
            ProjectJpaEntity entity = ProjectJpaEntity.toEntity(existing, team);
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
