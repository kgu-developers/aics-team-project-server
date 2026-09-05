package kgu.developers.api.meetingrecord.presentation;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import kgu.developers.api.meetingrecord.application.MeetingActionFacade;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionResponse;
import kgu.developers.api.meetingrecord.presentation.response.TeamMeetingActionListResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeetingActionControllerImpl implements MeetingActionController {

    private final MeetingActionFacade meetingActionFacade;

    @Override
    @GetMapping("/meeting-records/{meetingRecordId}/actions")
    public ResponseEntity<MeetingActionListResponse> getMeetingActions(
        @PathVariable Long meetingRecordId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(meetingActionFacade.getMeetingActions(meetingRecordId, authentication.getName()));
    }

    @Override
    @PostMapping("/meeting-records/{meetingRecordId}/actions")
    public ResponseEntity<MeetingActionResponse> createMeetingAction(
        @PathVariable Long meetingRecordId,
        @Valid @RequestBody MeetingActionCreateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(CREATED)
            .body(meetingActionFacade.createMeetingAction(meetingRecordId, authentication.getName(), request));
    }

    @Override
    @PatchMapping("/meeting-actions/{id}")
    public ResponseEntity<MeetingActionResponse> updateMeetingAction(
        @PathVariable Long id,
        @Valid @RequestBody MeetingActionUpdateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(meetingActionFacade.updateMeetingAction(id, authentication.getName(), request));
    }

    @Override
    @GetMapping("/teams/{teamId}/actions")
    public ResponseEntity<TeamMeetingActionListResponse> getTeamActions(
        @PathVariable Long teamId,
        @RequestParam(required = false) MeetingActionStatus status,
        Authentication authentication
    ) {
        return ResponseEntity.ok(meetingActionFacade.getTeamActions(teamId, status, authentication.getName()));
    }
}
