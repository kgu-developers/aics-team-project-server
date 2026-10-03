package kgu.developers.admin.meetingrecord.presentation;

import kgu.developers.admin.meetingrecord.application.MeetingRecordEditLogAdminFacade;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordEditLogAdminPageResponse;
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
@RequestMapping("/api/v1/admin/sections/{sectionId}/meeting-records/logs")
public class MeetingRecordEditLogAdminControllerImpl implements MeetingRecordEditLogAdminController {

    private final MeetingRecordEditLogAdminFacade meetingRecordEditLogAdminFacade;

    @Override
    @GetMapping
    public ResponseEntity<MeetingRecordEditLogAdminPageResponse> getSectionMeetingRecordLogs(
        @PathVariable Long sectionId,
        @RequestParam(required = false) Long teamId,
        @RequestParam(required = false) Long meetingRecordId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            meetingRecordEditLogAdminFacade.getSectionMeetingRecordLogs(
                sectionId, teamId, meetingRecordId, PageRequest.of(page, size), authentication.getName()));
    }
}
