package kgu.developers.domain.meetingrecord.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeetingParticipantRepository extends JpaRepository<MeetingParticipantJpaEntity, Long> {

    List<MeetingParticipantJpaEntity> findAllByMeetingRecordId(Long meetingRecordId);

    List<MeetingParticipantJpaEntity> findAllByMeetingRecordIdIn(List<Long> meetingRecordIds);

    void deleteAllByMeetingRecordId(Long meetingRecordId);
}
