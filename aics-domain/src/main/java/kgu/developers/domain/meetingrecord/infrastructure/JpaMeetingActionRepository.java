package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeetingActionRepository extends JpaRepository<MeetingActionJpaEntity, Long> {

    List<MeetingActionJpaEntity> findAllByMeetingRecordId(Long meetingRecordId);
}
