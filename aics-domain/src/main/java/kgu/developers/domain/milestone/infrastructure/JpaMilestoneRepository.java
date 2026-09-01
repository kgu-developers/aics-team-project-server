package kgu.developers.domain.milestone.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMilestoneRepository extends JpaRepository<MilestoneJpaEntity, Long> {

    Optional<MilestoneJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<MilestoneJpaEntity> findByIdAndSectionIdAndDeletedAtIsNull(Long id, Long sectionId);

    boolean existsBySectionIdAndWeekNumberAndDeletedAtIsNull(Long sectionId, int weekNumber);

    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT milestone
            FROM MilestoneJpaEntity milestone
            WHERE milestone.id = :id
              AND milestone.sectionId = :sectionId
              AND milestone.deletedAt IS NULL
            """)
    Optional<MilestoneJpaEntity> findActiveByIdAndSectionIdForUpdate(
            @Param("id") Long id,
            @Param("sectionId") Long sectionId
    );

    List<MilestoneJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(Long sectionId);

    List<MilestoneJpaEntity> findAllBySectionIdAndStatusAndDeletedAtIsNullOrderByWeekNumberAsc(
            Long sectionId,
            MilestoneStatus status
    );
}
