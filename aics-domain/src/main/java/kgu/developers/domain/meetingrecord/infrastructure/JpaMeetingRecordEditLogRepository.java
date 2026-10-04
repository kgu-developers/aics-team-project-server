package kgu.developers.domain.meetingrecord.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMeetingRecordEditLogRepository extends JpaRepository<MeetingRecordEditLogJpaEntity, Long> {

    Page<MeetingRecordEditLogJpaEntity> findAllByMeetingRecordId(Long meetingRecordId, Pageable pageable);

    @Query("""
        select l from MeetingRecordEditLogJpaEntity l
        where l.teamId in :teamIds
        and (:meetingRecordId is null or l.meetingRecordId = :meetingRecordId)
        """)
    Page<MeetingRecordEditLogJpaEntity> findAllByTeamIdIn(
        @Param("teamIds") List<Long> teamIds,
        @Param("meetingRecordId") Long meetingRecordId,
        Pageable pageable
    );

    @Query("""
        select l.teamId as teamId, count(l) as editCount
        from MeetingRecordEditLogJpaEntity l
        where l.teamId in :teamIds
        and l.createdAt <= :until
        group by l.teamId
        """)
    List<EditCountProjection> countByTeamIdInUntil(
        @Param("teamIds") List<Long> teamIds,
        @Param("until") LocalDateTime until
    );

    void deleteAllByMeetingRecordId(Long meetingRecordId);

    interface EditCountProjection {

        Long getTeamId();

        Long getEditCount();
    }
}
