package kgu.developers.domain.projectApproval.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kgu.developers.domain.projectApproval.domain.ApprovalCount;

public interface JpaProjectApprovalRepository extends JpaRepository<ProjectApprovalJpaEntity, Long> {
    Optional<ProjectApprovalJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(Long projectId);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndProposalRevisionAndDeletedAtIsNullOrderByUserIdAsc(Long projectId, long proposalRevision);

    // 무효화된 동의 재활성화 경쟁 방지: 조회 시점에 행을 잠근다 (호출자 트랜잭션 필수)
    @Lock(PESSIMISTIC_WRITE)
    Optional<ProjectApprovalJpaEntity> findByProjectIdAndUserIdAndProposalRevision(Long projectId, String userId, long proposalRevision);

    @Query("""
        SELECT new kgu.developers.domain.projectApproval.domain.ApprovalCount(COUNT(tm.id), COUNT(pa.id))
        FROM TeamMemberJpaEntity tm
        LEFT JOIN ProjectApprovalJpaEntity pa
            ON pa.userId = tm.user.studentNumber
            AND pa.projectId = :projectId
            AND pa.proposalRevision = :proposalRevision
            AND pa.deletedAt IS NULL
        WHERE tm.team.id = :teamId AND tm.deletedAt IS NULL
        """)
    ApprovalCount countApprovalsByTeamMembers(
        @Param("projectId") Long projectId,
        @Param("teamId") Long teamId,
        @Param("proposalRevision") long proposalRevision
    );

    @Modifying
    @Query("UPDATE ProjectApprovalJpaEntity p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.projectId = :projectId AND p.deletedAt IS NULL")
    int softDeleteAllByProjectId(@Param("projectId") Long projectId);
}
