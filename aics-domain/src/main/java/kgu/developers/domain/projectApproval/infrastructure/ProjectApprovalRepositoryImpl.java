package kgu.developers.domain.projectApproval.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.projectApproval.domain.ApprovalCount;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProjectApprovalRepositoryImpl implements ProjectApprovalRepository {
    private final JpaProjectApprovalRepository jpaProjectApprovalRepository;

    @Override
    public ProjectApproval save(ProjectApproval projectApproval) {
        ProjectApprovalJpaEntity entity = ProjectApprovalJpaEntity.toEntity(projectApproval);
        return jpaProjectApprovalRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<ProjectApproval> findById(Long id) {
        return jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(id)
            .map(ProjectApprovalJpaEntity::toDomain);
    }

    @Override
    public boolean existsByProjectIdAndUserIdAndProposalRevision(Long projectId, String userId, long proposalRevision) {
        return jpaProjectApprovalRepository
            .existsByProjectIdAndUserIdAndProposalRevisionAndDeletedAtIsNull(projectId, userId, proposalRevision);
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
        jpaProjectApprovalRepository.findByIdAndDeletedAtIsNull(id)
            .ifPresent(approval -> {
                approval.delete();
                jpaProjectApprovalRepository.saveAndFlush(approval);
            });
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        jpaProjectApprovalRepository.softDeleteAllByProjectId(projectId);
    }
}
