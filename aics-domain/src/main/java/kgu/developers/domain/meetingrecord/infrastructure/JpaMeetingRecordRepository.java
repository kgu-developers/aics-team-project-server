package kgu.developers.domain.meetingrecord.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMeetingRecordRepository extends JpaRepository<MeetingRecordJpaEntity, Long> {

    List<MeetingRecordJpaEntity> findAllByTeamId(Long teamId);

    List<MeetingRecordJpaEntity> findAllByTeamIdAndPhase(Long teamId, MeetingPhase phase);

    Page<MeetingRecordJpaEntity> findAllByTeamIdIn(List<Long> teamIds, Pageable pageable);

    @Query("""
        select m from MeetingRecordJpaEntity m
        where m.teamId in :teamIds
          and exists (
              select 1 from MeetingRecordMilestoneJpaEntity link
              where link.meetingRecordId = m.id and link.milestoneId = :milestoneId
          )
        """)
    Page<MeetingRecordJpaEntity> findAllByTeamIdInAndMilestoneId(
        @Param("teamIds") List<Long> teamIds,
        @Param("milestoneId") Long milestoneId,
        Pageable pageable
    );

    @Query("""
        select count(m) from MeetingRecordJpaEntity m
        where m.teamId = :teamId
          and exists (
              select 1 from MeetingRecordMilestoneJpaEntity link
              where link.meetingRecordId = m.id and link.milestoneId = :milestoneId
          )
        """)
    long countByTeamIdAndMilestoneId(
        @Param("teamId") Long teamId,
        @Param("milestoneId") Long milestoneId
    );

    @Query("""
        select m.teamId as teamId, count(m) as meetingRecordCount
        from MeetingRecordJpaEntity m
        where m.teamId in :teamIds
          and exists (
              select 1 from MeetingRecordMilestoneJpaEntity link
              where link.meetingRecordId = m.id and link.milestoneId = :milestoneId
          )
        group by m.teamId
        """)
    List<MeetingRecordCountProjection> countByTeamIdInAndMilestoneId(
        @Param("teamIds") List<Long> teamIds,
        @Param("milestoneId") Long milestoneId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MeetingRecordJpaEntity m where m.id = :id")
    Optional<MeetingRecordJpaEntity> findByIdForUpdate(@Param("id") Long id);

    interface MeetingRecordCountProjection {

        Long getTeamId();

        long getMeetingRecordCount();
    }
}
