package kgu.developers.domain.meetingrecord.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingRecordRepository {

    MeetingRecord save(MeetingRecord meetingRecord);

    Optional<MeetingRecord> findById(Long id);

    List<MeetingRecord> findAllByTeamId(Long teamId, MeetingPhase phase);

    Page<MeetingRecord> findAllByTeamIdIn(List<Long> teamIds, Pageable pageable);

    void deleteById(Long id);
}
