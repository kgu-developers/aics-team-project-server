package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeetingRecordRepository extends JpaRepository<MeetingRecordJpaEntity, Long> {

    List<MeetingRecordJpaEntity> findAllByTeamId(Long teamId);

    List<MeetingRecordJpaEntity> findAllByTeamIdAndPhase(Long teamId, MeetingPhase phase);
}
