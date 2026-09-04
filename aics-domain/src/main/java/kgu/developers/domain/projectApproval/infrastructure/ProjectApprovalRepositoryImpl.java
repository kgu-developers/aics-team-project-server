package kgu.developers.domain.projectApproval.infrastructure;

import java.util.List;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import kgu.developers.domain.projectApproval.domain.ApprovalCount;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import kgu.developers.domain.projectApproval.exception.ProjectApprovalNotFoundException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProjectApprovalRepositoryImpl implements ProjectApprovalRepository {
    private static final String UNIQUE_CONSTRAINT_NAME = "uk_project_approval_project_user";

    private final JpaProjectApprovalRepository jpaProjectApprovalRepository;

    @Override
    public ProjectApproval save(ProjectApproval projectApproval) {
        ProjectApprovalJpaEntity entity = ProjectApprovalJpaEntity.toEntity(projectApproval);
        try {
            return jpaProjectApprovalRepository.saveAndFlush(entity).toDomain();
        } catch (DataIntegrityViolationException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new DuplicateProjectApprovalException(e);
            }
            throw e;
        }
    }

    private boolean isUniqueConstraintViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                && UNIQUE_CONSTRAINT_NAME.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase().contains(UNIQUE_CONSTRAINT_NAME)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<ProjectApproval> findById(Long id) {
        return jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(id)
            .map(ProjectApprovalJpaEntity::toDomain);
    }

    @Override
    public Optional<ProjectApproval> findIncludingDeleted(Long projectId, String userId, long proposalRevision) {
        return jpaProjectApprovalRepository
            .findByProjectIdAndUserIdAndProposalRevision(projectId, userId, proposalRevision)
            .map(ProjectApprovalJpaEntity::toDomain);
    }

    @Override
    public List<ProjectApproval> findAllByProjectId(Long projectId) {
        return jpaProjectApprovalRepository.findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(projectId)
            .stream()
            .map(ProjectApprovalJpaEntity::toDomain)
            .toList();
    }

    @Override
    public List<ProjectApproval> findAllByProjectIdAndProposalRevision(Long projectId, long proposalRevision) {
        return jpaProjectApprovalRepository
            .findAllByProjectIdAndProposalRevisionAndDeletedAtIsNullOrderByUserIdAsc(projectId, proposalRevision)
            .stream()
            .map(ProjectApprovalJpaEntity::toDomain)
            .toList();
    }

    @Override
    public ApprovalCount countApprovalsByTeamMembers(Long projectId, Long teamId, long proposalRevision) {
        return jpaProjectApprovalRepository.countApprovalsByTeamMembers(projectId, teamId, proposalRevision);
    }

    @Override
    public void deleteById(Long id) {
        ProjectApprovalJpaEntity projectApproval = jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(ProjectApprovalNotFoundException::new);
        projectApproval.delete();
        jpaProjectApprovalRepository.saveAndFlush(projectApproval);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        jpaProjectApprovalRepository.softDeleteAllByProjectId(projectId);
    }
}
