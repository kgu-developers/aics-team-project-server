package kgu.developers.api.meetingrecord.presentation;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import kgu.developers.api.meetingrecord.application.MeetingRecordFacade;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordDetailResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordPersistResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MeetingRecordControllerImpl implements MeetingRecordController {

    private final MeetingRecordFacade meetingRecordFacade;

    @Override
    @GetMapping("/teams/{teamId}/meeting-records")
    public ResponseEntity<MeetingRecordListResponse> getMeetingRecords(
        @PathVariable Long teamId,
        @RequestParam(required = false) MeetingPhase phase,
        Authentication authentication
    ) {
        return ResponseEntity.ok(meetingRecordFacade.getMeetingRecords(teamId, phase, authentication.getName()));
    }

    @Override
    @PostMapping("/teams/{teamId}/meeting-records")
    public ResponseEntity<MeetingRecordPersistResponse> createMeetingRecord(
        @PathVariable Long teamId,
        @Valid @RequestBody MeetingRecordCreateRequest request,
        Authentication authentication
    ) {
        String authorId = authentication.getName();
        return ResponseEntity.status(CREATED).body(meetingRecordFacade.createMeetingRecord(teamId, authorId, request));
    }

    @Override
    @GetMapping("/meeting-records/{id}")
    public ResponseEntity<MeetingRecordDetailResponse> getMeetingRecord(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(meetingRecordFacade.getMeetingRecord(id, authentication.getName()));
    }

    @Override
    @PatchMapping("/meeting-records/{id}")
    public ResponseEntity<MeetingRecordPersistResponse> updateMeetingRecord(
        @PathVariable Long id,
        @Valid @RequestBody MeetingRecordUpdateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(meetingRecordFacade.updateMeetingRecord(id, request, authentication.getName()));
    }

    @Override
    @DeleteMapping("/meeting-records/{id}")
    public ResponseEntity<Void> deleteMeetingRecord(@PathVariable Long id, Authentication authentication) {
        meetingRecordFacade.deleteMeetingRecord(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
