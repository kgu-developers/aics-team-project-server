package kgu.developers.domain.meetingrecord.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingRecordRepository {

    MeetingRecord save(MeetingRecord meetingRecord);

    Optional<MeetingRecord> findById(Long id);

    List<MeetingRecord> findAllByTeamId(Long teamId, MeetingPhase phase);

    List<MeetingRecord> findAllByIdIn(List<Long> ids);

    Page<MeetingRecord> findAllByTeamIdIn(List<Long> teamIds, Pageable pageable);

    Page<MeetingRecord> findAllByTeamIdInAndMilestoneId(List<Long> teamIds, Long milestoneId, Pageable pageable);

    long countByTeamIdAndMilestoneId(Long teamId, Long milestoneId);

    Map<Long, Long> countByTeamIdInAndMilestoneId(List<Long> teamIds, Long milestoneId);

    void deleteById(Long id);
}
