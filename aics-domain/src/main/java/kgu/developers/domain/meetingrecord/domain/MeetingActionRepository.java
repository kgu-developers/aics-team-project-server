package kgu.developers.domain.meetingrecord.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingActionRepository {

    MeetingAction save(MeetingAction meetingAction);

    Optional<MeetingAction> findById(Long id);

    List<MeetingAction> findAllByMeetingRecordId(Long meetingRecordId);

    List<MeetingAction> findAllByTeamId(Long teamId, MeetingActionStatus status);

    Page<MeetingAction> findAllByTeamIdIn(
        List<Long> teamIds,
        Long meetingRecordId,
        MeetingActionStatus status,
        Pageable pageable
    );

    void deleteById(Long id);
}
