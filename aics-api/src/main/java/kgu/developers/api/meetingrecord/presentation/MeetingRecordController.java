package kgu.developers.api.meetingrecord.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordDetailResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordPersistResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "MeetingRecord", description = "회의록 API")
public interface MeetingRecordController {

    @Operation(
        summary = "회의록 목록 조회 API",
        description = """
            Description : 팀의 회의록 목록을 phase(선택, 미지정 시 전체)로 필터링해 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MeetingRecordListResponse.class)))
    ResponseEntity<MeetingRecordListResponse> getMeetingRecords(
        @PathVariable Long teamId,
        @RequestParam(required = false) MeetingPhase phase
    );

    @Operation(
        summary = "회의록 생성 API",
        description = """
            Description : 팀 회의록을 생성하고 참석자 목록을 함께 등록한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = MeetingRecordPersistResponse.class)))
    ResponseEntity<MeetingRecordPersistResponse> createMeetingRecord(
        @PathVariable Long teamId,
        @Valid @RequestBody MeetingRecordCreateRequest request
    );

    @Operation(
        summary = "회의록 상세 조회 API",
        description = """
            Description : 회의록 상세 내용과 참석자 학번 전체 목록을 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MeetingRecordDetailResponse.class)))
    ResponseEntity<MeetingRecordDetailResponse> getMeetingRecord(@PathVariable Long id);

    @Operation(
        summary = "회의록 수정 API",
        description = """
            Description : 회의록의 일부 필드를 수정한다. participantIds가 포함되면 참석자 목록 전체를 치환한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MeetingRecordPersistResponse.class)))
    ResponseEntity<MeetingRecordPersistResponse> updateMeetingRecord(
        @PathVariable Long id,
        @Valid @RequestBody MeetingRecordUpdateRequest request
    );

    @Operation(
        summary = "회의록 삭제 API",
        description = """
            Description : 회의록을 삭제한다. (삭제 정책 미확정 — 현재는 하드 삭제로 구현)
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> deleteMeetingRecord(@PathVariable Long id);
}
