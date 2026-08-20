package kgu.developers.domain.meetingrecord.application.query;

import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingRecordQueryService {

    private final MeetingRecordRepository meetingRecordRepository;

    public MeetingRecord getMeetingRecord(Long id) {
        return meetingRecordRepository.findById(id)
            .orElseThrow(MeetingRecordNotFoundException::new);
    }

    public List<MeetingRecord> getMeetingRecords(Long teamId, MeetingPhase phase) {
        return meetingRecordRepository.findAllByTeamId(teamId, phase);
    }
}
