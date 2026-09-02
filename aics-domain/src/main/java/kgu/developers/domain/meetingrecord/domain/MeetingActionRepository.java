package kgu.developers.domain.meetingrecord.domain;

import java.util.List;
import java.util.Optional;

public interface MeetingActionRepository {

    MeetingAction save(MeetingAction meetingAction);

    Optional<MeetingAction> findById(Long id);

    List<MeetingAction> findAllByMeetingRecordId(Long meetingRecordId);

    List<MeetingAction> findAllByTeamId(Long teamId, MeetingActionStatus status);

    void deleteById(Long id);
}
