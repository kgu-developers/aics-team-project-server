package kgu.developers.admin.meetingrecord.presentation;

import kgu.developers.admin.meetingrecord.application.MeetingRecordAdminFacade;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordAdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/meeting-records")
public class MeetingRecordAdminControllerImpl implements MeetingRecordAdminController {

    private final MeetingRecordAdminFacade meetingRecordAdminFacade;

    @Override
    @GetMapping
    public ResponseEntity<MeetingRecordAdminPageResponse> getMeetingRecords(
        @RequestParam(required = false) Long sectionId,
        @PageableDefault(size = 20, sort = "meetingAt", direction = Sort.Direction.DESC) Pageable pageable,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            meetingRecordAdminFacade.getMeetingRecords(sectionId, pageable, authentication.getName()));
    }
}
