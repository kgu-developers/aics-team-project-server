package kgu.developers.domain.projectApproval.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProjectApprovalRepository extends JpaRepository<ProjectApprovalJpaEntity, Long> {
    Optional<ProjectApprovalJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    boolean existsByProjectIdAndUserIdAndProposalRevisionAndDeletedAtIsNull(Long projectId, String userId, long proposalRevision);

    Optional<ProjectApprovalJpaEntity> findByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(Long projectId);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndProposalRevisionAndDeletedAtIsNullOrderByUserIdAsc(Long projectId, long proposalRevision);

    List<ProjectApprovalJpaEntity> findAllByUserIdAndDeletedAtIsNullOrderByProjectIdAsc(String userId);
}
