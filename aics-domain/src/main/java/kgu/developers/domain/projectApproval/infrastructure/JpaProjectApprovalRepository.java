package kgu.developers.domain.projectApproval.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProjectApprovalRepository extends JpaRepository<ProjectApprovalJpaEntity, Long> {
    Optional<ProjectApprovalJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    Optional<ProjectApprovalJpaEntity> findByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(Long projectId);

    List<ProjectApprovalJpaEntity> findAllByUserIdAndDeletedAtIsNullOrderByProjectIdAsc(String userId);

    Optional<ProjectApprovalJpaEntity> findByProjectIdAndUserId(Long projectId, String userId);
}
