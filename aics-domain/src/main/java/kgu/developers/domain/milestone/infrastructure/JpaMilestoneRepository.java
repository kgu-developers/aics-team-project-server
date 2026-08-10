package kgu.developers.domain.milestone.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMilestoneRepository extends JpaRepository<MilestoneJpaEntity, Long> {

    Optional<MilestoneJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT milestone
            FROM MilestoneJpaEntity milestone
            WHERE milestone.id = :id AND milestone.deletedAt IS NULL
            """)
    Optional<MilestoneJpaEntity> findActiveByIdForUpdate(@Param("id") Long id);

    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT milestone
            FROM MilestoneJpaEntity milestone
            WHERE milestone.id IN :ids AND milestone.deletedAt IS NULL
            ORDER BY milestone.id
            """)
    List<MilestoneJpaEntity> findAllActiveByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT milestone
            FROM MilestoneJpaEntity milestone
            WHERE milestone.sectionId = :sectionId AND milestone.deletedAt IS NULL
            ORDER BY milestone.weekNumber, milestone.id
            """)
    List<MilestoneJpaEntity> findAllActiveBySectionIdForUpdate(@Param("sectionId") Long sectionId);

    List<MilestoneJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(Long sectionId);

    List<MilestoneJpaEntity> findAllBySectionIdAndStatusAndDeletedAtIsNullOrderByWeekNumberAsc(
            Long sectionId,
            MilestoneStatus status
    );
}
