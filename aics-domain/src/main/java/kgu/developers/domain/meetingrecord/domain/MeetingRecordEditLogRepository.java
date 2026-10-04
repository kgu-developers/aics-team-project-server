package kgu.developers.domain.meetingrecord.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingRecordEditLogRepository {

    MeetingRecordEditLog save(MeetingRecordEditLog meetingRecordEditLog);

    Page<MeetingRecordEditLog> findAllByMeetingRecordId(Long meetingRecordId, Pageable pageable);

    Page<MeetingRecordEditLog> findAllByTeamIdIn(List<Long> teamIds, Long meetingRecordId, Pageable pageable);

    // 분반 산출물 엑셀의 '회의록 수정 로그 수'는 기준일(until)까지 쌓인 것만 센다.
    Map<Long, Long> countByTeamIdInUntil(List<Long> teamIds, LocalDateTime until);

    void deleteAllByMeetingRecordId(Long meetingRecordId);
}
