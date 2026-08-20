package kgu.developers.domain.meetingrecord.domain;

import java.util.List;
import java.util.Optional;

public interface MeetingRecordRepository {

    MeetingRecord save(MeetingRecord meetingRecord);

    Optional<MeetingRecord> findById(Long id);

    List<MeetingRecord> findAllByTeamId(Long teamId, MeetingPhase phase);

    void deleteById(Long id);
}
