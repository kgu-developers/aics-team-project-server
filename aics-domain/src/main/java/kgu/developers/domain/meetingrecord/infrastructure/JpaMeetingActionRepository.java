package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;

import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 분반 단위 통합 조회는 담당 분반의 팀 전체를 한 번에 훑으므로 teamId 목록으로 받는다.
    // 회의록·상태 필터는 선택값이라 null이면 조건 자체를 건너뛴다.
    @Query(value = """
        select a from MeetingActionJpaEntity a
        join MeetingRecordJpaEntity m on a.meetingRecordId = m.id
        where m.teamId in :teamIds
        and (:meetingRecordId is null or a.meetingRecordId = :meetingRecordId)
        and (:status is null or a.status = :status)
        """,
        countQuery = """
        select count(a) from MeetingActionJpaEntity a
        join MeetingRecordJpaEntity m on a.meetingRecordId = m.id
        where m.teamId in :teamIds
        and (:meetingRecordId is null or a.meetingRecordId = :meetingRecordId)
        and (:status is null or a.status = :status)
        """)
    Page<MeetingActionJpaEntity> findAllByTeamIdIn(
        @Param("teamIds") List<Long> teamIds,
        @Param("meetingRecordId") Long meetingRecordId,
        @Param("status") MeetingActionStatus status,
        Pageable pageable
    );

    void deleteAllByMeetingRecordId(Long meetingRecordId);

}
