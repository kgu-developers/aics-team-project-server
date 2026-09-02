package kgu.developers.admin.meetingrecord.presentation;

import kgu.developers.admin.meetingrecord.application.MeetingRecordAdminFacade;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordAdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/meeting-records")
public class MeetingRecordAdminControllerImpl implements MeetingRecordAdminController {

    private final MeetingRecordAdminFacade meetingRecordAdminFacade;

    @Override
    @GetMapping
    public ResponseEntity<MeetingRecordAdminPageResponse> getMeetingRecords(
        @RequestParam(required = false) Long sectionId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            meetingRecordAdminFacade.getMeetingRecords(
                sectionId, PageRequest.of(page, size), authentication.getName()));
    }
}
