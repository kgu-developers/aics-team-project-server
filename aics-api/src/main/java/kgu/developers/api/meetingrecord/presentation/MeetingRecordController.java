package kgu.developers.api.meetingrecord.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingRecordUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordDetailResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordEditLogPageResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingRecordPersistResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        @RequestParam(required = false) MeetingPhase phase,
        Authentication authentication
    );

    @Operation(
        summary = "회의록 생성 API",
        description = """
            Description : 팀 회의록을 생성하고 참석자 목록을 함께 등록한다. 작성자는 요청 값이 아니라 인증된 사용자로 기록된다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = MeetingRecordPersistResponse.class)))
    ResponseEntity<MeetingRecordPersistResponse> createMeetingRecord(
        @PathVariable Long teamId,
        @Valid @RequestBody MeetingRecordCreateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "회의록 상세 조회 API",
        description = """
            Description : 회의록 상세 내용과 참석자 학번 전체 목록을 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MeetingRecordDetailResponse.class)))
    ResponseEntity<MeetingRecordDetailResponse> getMeetingRecord(@PathVariable Long id, Authentication authentication);

    @Operation(
        summary = "회의록 수정 API",
        description = """
            Description : 회의록의 일부 필드를 수정한다. participantIds가 포함되면 참석자 목록 전체를 치환한다.
                수정 사유(reason)는 필수이며 앞뒤 공백을 제외하고 30자 이상 500자 이하여야 한다.
                수정에 성공하면 사유가 수정 이력으로 적재되어 팀원과 담당 교수가 조회할 수 있다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MeetingRecordPersistResponse.class)))
    ResponseEntity<MeetingRecordPersistResponse> updateMeetingRecord(
        @PathVariable Long id,
        @Valid @RequestBody MeetingRecordUpdateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "회의록 수정 이력 조회 API",
        description = """
            Description : 회의록 수정 시 남긴 사유를 최신순으로 조회한다. 팀원과 담당 교수 모두 조회할 수 있다.
                수정 이력은 쌓기만 하며 수정·삭제할 수 없고, 회의록이 삭제되면 함께 사라진다.
            Assignee : 황호찬
            """
    )
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = MeetingRecordEditLogPageResponse.class))
    )
    ResponseEntity<MeetingRecordEditLogPageResponse> getMeetingRecordLogs(
        @PathVariable Long meetingRecordId,
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @Parameter(description = "페이지 크기(최대 100)", example = "20")
        @RequestParam(defaultValue = "20") @Positive @Max(100) int size,
        Authentication authentication
    );

    @Operation(
        summary = "회의록 삭제 API",
        description = """
            Description : 회의록을 삭제한다. 삭제 정책은 하드 삭제로 확정되어 있어 DB에서 즉시 완전히 제거되며 복구할 수 없다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "204")
    ResponseEntity<Void> deleteMeetingRecord(@PathVariable Long id, Authentication authentication);
}
