package kgu.developers.domain.meetingrecord.application.command;

import java.time.LocalDateTime;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionRepository;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.exception.MeetingActionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MeetingActionCommandService {

    private final MeetingActionRepository meetingActionRepository;

    public Long createMeetingAction(
            Long meetingRecordId,
            String assigneeId,
            String content,
            MeetingActionStatus status,
            LocalDateTime dueAt) {
        MeetingAction meetingAction = MeetingAction.create(meetingRecordId, assigneeId, content, status, dueAt);
        return meetingActionRepository.save(meetingAction).getId();
    }

    public void updateMeetingAction(
            Long id,
            String assigneeId,
            String content,
            MeetingActionStatus status,
            LocalDateTime dueAt
    ) {
        MeetingAction meetingAction = findOrThrow(id);

        if(content != null) {
            meetingAction.updateContent(content);
        }
        if (status != null) {
            meetingAction.updateStatus(status);
        }
        if (assigneeId != null) {
            meetingAction.updateAssigneeId(assigneeId);
        }
        if (dueAt != null) {
            meetingAction.updateDueAt(dueAt);
        }

        meetingActionRepository.save(meetingAction);
    }

    private MeetingAction findOrThrow(Long id) {
        return meetingActionRepository.findById(id)
                .orElseThrow(MeetingActionNotFoundException::new);
    }
}
