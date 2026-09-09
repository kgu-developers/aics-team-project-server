package kgu.developers.domain.midreport.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMidReportRepository extends JpaRepository<MidReportJpaEntity, Long> {
    @Query("select distinct report from MidReportJpaEntity report left join fetch report.blocks where report.id = :id and report.deletedAt is null")
    Optional<MidReportJpaEntity> findActiveById(@Param("id") Long id);

    @Query("select distinct report from MidReportJpaEntity report left join fetch report.blocks where report.teamId = :teamId and report.milestoneId = :milestoneId and report.deletedAt is null")
    Optional<MidReportJpaEntity> findActiveByTeamIdAndMilestoneId(
        @Param("teamId") Long teamId,
        @Param("milestoneId") Long milestoneId
    );
}
