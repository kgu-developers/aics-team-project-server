package kgu.developers.domain.meetingrecord.application.query;

import java.util.List;
import java.util.Map;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public List<MeetingRecord> getMeetingRecords(List<Long> ids) {
        return meetingRecordRepository.findAllByIdIn(ids);
    }

    public Page<MeetingRecord> getMeetingRecords(List<Long> teamIds, Pageable pageable) {
        return meetingRecordRepository.findAllByTeamIdIn(teamIds, pageable);
    }

    public Page<MeetingRecord> getMeetingRecords(List<Long> teamIds, Long milestoneId, Pageable pageable) {
        return milestoneId == null
            ? meetingRecordRepository.findAllByTeamIdIn(teamIds, pageable)
            : meetingRecordRepository.findAllByTeamIdInAndMilestoneId(teamIds, milestoneId, pageable);
    }

    public long countMeetingRecords(Long teamId, Long milestoneId) {
        return meetingRecordRepository.countByTeamIdAndMilestoneId(teamId, milestoneId);
    }

    public Map<Long, Long> countMeetingRecords(List<Long> teamIds, Long milestoneId) {
        return meetingRecordRepository.countByTeamIdInAndMilestoneId(teamIds, milestoneId);
    }

    public Map<Long, Long> countMeetingRecords(List<Long> teamIds) {
        return meetingRecordRepository.countByTeamIdIn(teamIds);
    }
}
