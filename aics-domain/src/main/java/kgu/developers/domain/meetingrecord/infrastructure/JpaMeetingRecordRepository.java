package kgu.developers.domain.meetingrecord.infrastructure;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
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

    @Query("""
        select m.teamId as teamId, count(m) as meetingRecordCount
        from MeetingRecordJpaEntity m
        where m.teamId in :teamIds
        group by m.teamId
        """)
    List<MeetingRecordCountProjection> countByTeamIdIn(
        @Param("teamIds") List<Long> teamIds
    );

    // 분반 산출물 현황은 본문 없이 팀별 건수·version 합만 쓴다. 엔티티를 적재하지 않으므로
    // 회의록이 몇 건이든 한 번에 집계된다. createdAt이 비어 있는 행은 기준일 필터에서 빼지 않는다.
    @Query("""
        select m.teamId as teamId, count(m) as meetingRecordCount, sum(m.version) as editCount
        from MeetingRecordJpaEntity m
        where m.teamId in :teamIds
          and (m.createdAt is null or m.createdAt <= :until)
        group by m.teamId
        """)
    List<MeetingRecordStatsProjection> statsByTeamIdInUntil(
        @Param("teamIds") List<Long> teamIds,
        @Param("until") LocalDateTime until
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MeetingRecordJpaEntity m where m.id = :id")
    Optional<MeetingRecordJpaEntity> findByIdForUpdate(@Param("id") Long id);

    interface MeetingRecordStatsProjection {

        Long getTeamId();

        long getMeetingRecordCount();

        long getEditCount();
    }

    interface MeetingRecordCountProjection {

        Long getTeamId();

        long getMeetingRecordCount();
    }
}
