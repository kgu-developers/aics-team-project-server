package kgu.developers.domain.meetingrecord.application.query;

import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLog;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordEditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingRecordEditLogQueryService {

    private final MeetingRecordEditLogRepository meetingRecordEditLogRepository;

    public Page<MeetingRecordEditLog> getMeetingRecordLogs(Long meetingRecordId, Pageable pageable) {
        return meetingRecordEditLogRepository.findAllByMeetingRecordId(meetingRecordId, pageable);
    }

    public Page<MeetingRecordEditLog> getSectionLogs(List<Long> teamIds, Long meetingRecordId, Pageable pageable) {
        return meetingRecordEditLogRepository.findAllByTeamIdIn(teamIds, meetingRecordId, pageable);
    }
}
