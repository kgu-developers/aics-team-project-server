package kgu.developers.domain.projectApproval.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

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
    public boolean existsByProjectIdAndUserId(Long projectId, String userId) {
        return jpaProjectApprovalRepository.existsByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId);
    }

    @Override
    public Optional<ProjectApproval> findByProjectIdAndUserId(Long projectId, String userId) {
        return jpaProjectApprovalRepository.findByProjectIdAndUserIdAndDeletedAtIsNull(projectId, userId)
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
    public List<ProjectApproval> findAllByUserId(String userId) {
        return jpaProjectApprovalRepository.findAllByUserIdAndDeletedAtIsNullOrderByProjectIdAsc(userId)
            .stream()
            .map(ProjectApprovalJpaEntity::toDomain)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaProjectApprovalRepository.deleteById(id);
    }
}
