package kgu.developers.admin.meetingrecord.presentation;

import kgu.developers.admin.meetingrecord.application.MeetingActionAdminFacade;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingActionAdminPageResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sections/{sectionId}/meeting-actions")
public class MeetingActionAdminControllerImpl implements MeetingActionAdminController {

    private final MeetingActionAdminFacade meetingActionAdminFacade;

    @Override
    @GetMapping
    public ResponseEntity<MeetingActionAdminPageResponse> getSectionMeetingActions(
        @PathVariable Long sectionId,
        @RequestParam(required = false) Long teamId,
        @RequestParam(required = false) Long meetingRecordId,
        @RequestParam(required = false) MeetingActionStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            meetingActionAdminFacade.getSectionMeetingActions(
                sectionId, teamId, meetingRecordId, status, PageRequest.of(page, size), authentication.getName()));
    }
}
