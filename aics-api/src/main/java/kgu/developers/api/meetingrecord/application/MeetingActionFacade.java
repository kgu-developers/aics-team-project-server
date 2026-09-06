package kgu.developers.api.meetingrecord.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionResponse;
import kgu.developers.api.meetingrecord.presentation.response.TeamMeetingActionListResponse;
import kgu.developers.api.meetingrecord.presentation.response.TeamMeetingActionResponse;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.meetingrecord.application.command.MeetingActionCommandService;
import kgu.developers.domain.meetingrecord.application.query.MeetingActionQueryService;
import kgu.developers.domain.meetingrecord.application.query.MeetingRecordQueryService;
import kgu.developers.domain.meetingrecord.domain.MeetingAction;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import kgu.developers.domain.meetingrecord.exception.MeetingActionInvalidAssigneeException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
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
    private final TeamAccessValidator teamAccessValidator;
    private final UserQueryService userQueryService;

    public MeetingActionListResponse getMeetingActions(Long meetingRecordId, String userId) {
        MeetingRecord meetingRecord = meetingRecordQueryService.getMeetingRecord(meetingRecordId);
        teamAccessValidator.validateMembershipOrProfessor(meetingRecord.getTeamId(), userId);
        List<MeetingAction> meetingActions = meetingActionQueryService.getMeetingActions(meetingRecordId);
        return MeetingActionListResponse.from(meetingActions, resolveAssignees(meetingActions));
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
            request.dueAt()
        );
        MeetingAction meetingAction = meetingActionQueryService.getMeetingAction(id);
        return MeetingActionResponse.from(meetingAction, resolveAssignee(meetingAction.getAssigneeId()));
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
        MeetingAction updated = meetingActionQueryService.getMeetingAction(id);
        return MeetingActionResponse.from(updated, resolveAssignee(updated.getAssigneeId()));
    }

    public TeamMeetingActionListResponse getTeamActions(Long teamId, MeetingActionStatus status, String userId) {
        teamAccessValidator.validateMembershipOrProfessor(teamId, userId);
        List<MeetingAction> meetingActions = meetingActionQueryService.getTeamActions(teamId, status);

        Map<String, User> usersByStudentNumber = resolveAssignees(meetingActions);
        Map<Long, MeetingRecord> meetingRecordsById = meetingRecordQueryService.getMeetingRecords(
                meetingActions.stream().map(MeetingAction::getMeetingRecordId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(MeetingRecord::getId, Function.identity()));

        List<TeamMeetingActionResponse> contents = meetingActions.stream()
            .map(action -> TeamMeetingActionResponse.from(
                action,
                usersByStudentNumber.get(action.getAssigneeId()),
                meetingRecordsById.get(action.getMeetingRecordId())))
            .toList();

        return TeamMeetingActionListResponse.builder().contents(contents).build();
    }

    private User resolveAssignee(String assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userQueryService.getUsersByStudentNumbers(List.of(assigneeId)).stream().findFirst().orElse(null);
    }

    private Map<String, User> resolveAssignees(List<MeetingAction> meetingActions) {
        List<String> assigneeIds = meetingActions.stream()
            .map(MeetingAction::getAssigneeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (assigneeIds.isEmpty()) {
            return Map.of();
        }
        return userQueryService.getUsersByStudentNumbers(assigneeIds).stream()
            .collect(Collectors.toMap(User::getStudentNumber, Function.identity()));
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
