package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;

import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMeetingActionRepository extends JpaRepository<MeetingActionJpaEntity, Long> {

    List<MeetingActionJpaEntity> findAllByMeetingRecordId(Long meetingRecordId);

    @Query("""
        select a from MeetingActionJpaEntity a 
        join MeetingRecordJpaEntity m on a.meetingRecordId = m.id
        where m.teamId = :teamId
        and (:status is null or a.status = :status)
        """)
    List<MeetingActionJpaEntity> findAllByTeamId(@Param("teamId") Long teamId,
                                                 @Param("status")MeetingActionStatus status);

    void deleteAllByMeetingRecordId(Long meetingRecordId);

}
