package kgu.developers.api.meetingrecord.application;

import kgu.developers.api.meetingrecord.presentation.request.MeetingActionCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionResponse;
import kgu.developers.domain.meetingrecord.application.command.MeetingActionCommandService;
import kgu.developers.domain.meetingrecord.application.query.MeetingActionQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.exception.MeetingActionInvalidAssigneeException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class MeetingActionFacade {

    private final MeetingActionCommandService meetingActionCommandService;
    private final MeetingActionQueryService meetingActionQueryService;
    private final MeetingRecordQueryService meetingRecordQueryService;
    private final TeamMemberRepository teamMemberRepository;

    public MeetingActionListResponse getMeetingActions(Long meetingRecordId, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(meetingRecordId);
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        return MeetingActionListResponse.from(meetingActionQueryService.getMeetingActions(meetingRecordId));
    }

    public MeetingActionResponse createMeetingAction(Long meetingRecordId, String userId, MeetingActionCreateRequest request) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(meetingRecordId);
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        if (request.assigneeId() != null) {
            validateAssignee(meetingRecord.getTeamId(), request.assigneeId());
        }
        Long id = meetingActionCommandService.createMeetingAction(
            meetingRecordId,
            request.assigneeId(),
            request.content(),
            request.status(),
            request.dueAt()
        );
        return MeetingActionResponse.from(meetingActionQueryService.getMeetingAction(id));
    }

    public MeetingActionResponse updateMeetingAction(Long id, String userId, MeetingActionUpdateRequest request) {
        MeetingAction meetingAction = meetingActionQueryService.getMeetingAction(id);
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(meetingAction.getMeetingRecordId());
        validateTeamMembership(meetingRecord.getTeamId(), userId);
        if (!request.clearAssignee() && request.assigneeId() != null) {
            validateAssignee(meetingRecord.getTeamId(), request.assigneeId());
        }
        meetingActionCommandService.updateMeetingAction(
            id,
            request.assigneeId(),
            request.content(),
            request.status(),
            request.dueAt(),
            request.clearAssignee(),
            request.clearDueAt()
        );
        return MeetingActionResponse.from(meetingActionQueryService.getMeetingAction(id));
    }

    public MeetingActionListResponse getTeamActions(Long teamId, MeetingActionStatus status, String userId) {
        validateTeamMembership(teamId, userId);
        return MeetingActionListResponse.from(meetingActionQueryService.getTeamActions(teamId, status));
    }

    private void validateTeamMembership(Long teamId, String userId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, userId).isEmpty()) {
            throw new AccessDeniedException("해당 팀에 소속된 사용자만 액션플랜에 접근할 수 있습니다.");
        }
    }

    private void validateAssignee(Long teamId, String assigneeId) {
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, assigneeId).isEmpty()) {
            throw new MeetingActionInvalidAssigneeException();
        }
    }
}
