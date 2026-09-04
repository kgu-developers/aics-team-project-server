package kgu.developers.domain.teamMember.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaTeamMemberRepository extends JpaRepository<TeamMemberJpaEntity, Long> {
    Optional<TeamMemberJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    List<TeamMemberJpaEntity> findAllByTeamIdAndDeletedAtIsNull(Long teamId);

    List<TeamMemberJpaEntity> findAllByTeamIdInAndDeletedAtIsNull(List<Long> teamIds);

    List<TeamMemberJpaEntity> findAllByUserStudentNumberAndDeletedAtIsNull(String userId);

    Optional<TeamMemberJpaEntity> findByTeamIdAndUserStudentNumberAndDeletedAtIsNull(Long teamId, String userId);

    Optional<TeamMemberJpaEntity> findByTeamIdAndUserStudentNumber(Long teamId, String userId);

    Optional<TeamMemberJpaEntity> findByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(Long teamId);

    boolean existsByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(Long teamId);

    @Query("SELECT tm FROM TeamMemberJpaEntity tm WHERE tm.team.section.id = :sectionId AND tm.user.studentNumber = :userId AND tm.deletedAt IS NULL")
    Optional<TeamMemberJpaEntity> findBySectionIdAndUserStudentNumberAndDeletedAtIsNull(@Param("sectionId") Long sectionId, @Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TeamMemberJpaEntity tm set tm.deletedAt = :now, tm.updatedAt = :now"
            + " where tm.team.id = :teamId and tm.deletedAt is null")
    int softDeleteAllByTeamId(@Param("teamId") Long teamId, @Param("now") LocalDateTime now);
}
