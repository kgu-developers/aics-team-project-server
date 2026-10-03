package kgu.developers.domain.meetingrecord.domain;

import java.time.LocalDateTime;
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

    Map<Long, Long> countByTeamIdIn(List<Long> teamIds);

    // 기준일까지의 팀별 회의록 건수·수정 횟수 근사값. 본문을 적재하지 않는다.
    Map<Long, MeetingRecordStats> statsByTeamIdInUntil(List<Long> teamIds, LocalDateTime until);

    void deleteById(Long id);
}
