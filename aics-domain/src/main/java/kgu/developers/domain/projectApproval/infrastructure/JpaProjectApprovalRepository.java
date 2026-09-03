package kgu.developers.domain.projectApproval.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import kgu.developers.domain.projectApproval.domain.ApprovalCount;

public interface JpaProjectApprovalRepository extends JpaRepository<ProjectApprovalJpaEntity, Long> {
    Optional<ProjectApprovalJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    boolean existsByProjectIdAndUserIdAndProposalRevisionAndDeletedAtIsNull(Long projectId, String userId, long proposalRevision);

    Optional<ProjectApprovalJpaEntity> findByProjectIdAndUserIdAndDeletedAtIsNull(Long projectId, String userId);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndDeletedAtIsNullOrderByUserIdAsc(Long projectId);

    List<ProjectApprovalJpaEntity> findAllByProjectIdAndProposalRevisionAndDeletedAtIsNullOrderByUserIdAsc(Long projectId, long proposalRevision);

    List<ProjectApprovalJpaEntity> findAllByUserIdAndDeletedAtIsNullOrderByProjectIdAsc(String userId);

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
