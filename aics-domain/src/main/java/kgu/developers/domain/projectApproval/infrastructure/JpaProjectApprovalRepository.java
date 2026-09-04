package kgu.developers.domain.projectApproval.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface JpaProjectApprovalRepository extends JpaRepository<ProjectApprovalJpaEntity, Long> {
    Optional<ProjectApprovalJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    Optional<ProjectApprovalJpaEntity> findByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(Long projectId);

    List<ProjectApprovalJpaEntity> findAllByUserIdAndDeletedAtIsNullOrderByProjectIdAsc(String userId);

    // 삭제된 동의 재활성화 경쟁 방지: 조회 시점에 행을 잠근다 (호출자 트랜잭션 필수)
    @Lock(PESSIMISTIC_WRITE)
    Optional<ProjectApprovalJpaEntity> findByProjectIdAndUserId(Long projectId, String userId);
}
