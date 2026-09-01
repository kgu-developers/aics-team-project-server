package kgu.developers.domain.meetingrecord.application.query;

import java.util.List;

import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.exception.MeetingActionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingActionQueryService {

    private final MeetingActionRepository meetingActionRepository;

    public MeetingAction getMeetingAction(Long id) {
        return meetingActionRepository.findById(id)
                .orElseThrow(MeetingActionNotFoundException::new);
    }

    public List<MeetingAction> getMeetingActions(Long meetingRecordId) {
        return meetingActionRepository.findAllByMeetingRecordId(meetingRecordId);
    }

    public List<MeetingAction> getTeamActions(Long teamId, MeetingActionStatus status) {
        return meetingActionRepository.findAllByTeamId(teamId, status);
    }
}
