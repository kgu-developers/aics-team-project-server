package kgu.developers.domain.meetingrecord.application.command;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.domain.MeetingRecordRepository;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordInvalidContentException;
import kgu.developers.domain.meetingrecord.exception.MeetingRecordNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingRecordCommandService {

    private final MeetingRecordRepository meetingRecordRepository;

    public Long createMeetingRecord(
        Long teamId,
        MeetingPhase phase,
        String authorId,
        LocalDateTime meetingAt,
        String location,
        String content,
        List<String> participantIds
    ) {
        MeetingRecord meetingRecord = MeetingRecord.create(teamId, phase, authorId, meetingAt, location, content, participantIds);
        return meetingRecordRepository.save(meetingRecord).getId();
    }

    public void updateMeetingRecord(
        Long id,
        LocalDateTime meetingAt,
        String location,
        MeetingPhase phase,
        String content,
        List<String> participantIds
    ) {
        MeetingRecord meetingRecord = findOrThrow(id);

        if (meetingAt != null) {
            meetingRecord.updateMeetingAt(meetingAt);
        }
        if (location != null) {
            meetingRecord.updateLocation(location);
        }
        if (phase != null) {
            meetingRecord.updatePhase(phase);
        }
        if (content != null) {
            if (content.isBlank()) {
                throw new MeetingRecordInvalidContentException();
            }
            meetingRecord.updateContent(content);
        }
        if (participantIds != null) {
            meetingRecord.updateParticipants(participantIds);
        }

        meetingRecordRepository.save(meetingRecord);
    }

    // TODO: 정책이 소프트 삭제로 바뀌면 BaseTimeEntity.delete() 기반으로 전환한다.
    public void deleteMeetingRecord(Long id) {
        findOrThrow(id);
        meetingRecordRepository.deleteById(id);
    }

    private MeetingRecord findOrThrow(Long id) {
        return meetingRecordRepository.findById(id)
            .orElseThrow(MeetingRecordNotFoundException::new);
    }
}
