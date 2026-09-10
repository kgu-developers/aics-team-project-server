package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeetingRecordMilestoneRepository
    extends JpaRepository<MeetingRecordMilestoneJpaEntity, Long> {

    List<MeetingRecordMilestoneJpaEntity> findAllByMeetingRecordId(Long meetingRecordId);

    List<MeetingRecordMilestoneJpaEntity> findAllByMeetingRecordIdIn(List<Long> meetingRecordIds);

    void deleteAllByMeetingRecordId(Long meetingRecordId);
}
