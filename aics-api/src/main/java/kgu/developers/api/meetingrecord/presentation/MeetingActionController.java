package kgu.developers.api.meetingrecord.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionCreateRequest;
import kgu.developers.api.meetingrecord.presentation.request.MeetingActionUpdateRequest;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionListResponse;
import kgu.developers.api.meetingrecord.presentation.response.MeetingActionResponse;
import kgu.developers.api.meetingrecord.presentation.response.TeamMeetingActionListResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "MeetingAction", description = "액션플랜 API")
public interface MeetingActionController {

    @Operation(
        summary = "액션플랜 목록 조회 API",
        description = """
            Description : 특정 회의록에 속한 액션플랜 목록을 조회한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MeetingActionListResponse.class)))
    ResponseEntity<MeetingActionListResponse> getMeetingActions(
        @PathVariable Long meetingRecordId,
        Authentication authentication
    );

    @Operation(
        summary = "액션플랜 등록 API",
        description = """
            Description : 회의록에 새 액션플랜을 등록한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = MeetingActionResponse.class)))
    ResponseEntity<MeetingActionResponse> createMeetingAction(
        @PathVariable Long meetingRecordId,
        @Valid @RequestBody MeetingActionCreateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "액션플랜 수정 API",
        description = """
            Description : 액션플랜의 내용/상태/마감일/담당자 중 요청에 포함된 필드만 수정한다.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MeetingActionResponse.class)))
    ResponseEntity<MeetingActionResponse> updateMeetingAction(
        @PathVariable Long id,
        @Valid @RequestBody MeetingActionUpdateRequest request,
        Authentication authentication
    );

    @Operation(
        summary = "팀 전체 액션플랜 조회 API",
        description = """
            Description : 팀 전체의 액션플랜을 status(선택, 미지정 시 전체)로 필터링해 조회한다. 대시보드용.
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TeamMeetingActionListResponse.class)))
    ResponseEntity<TeamMeetingActionListResponse> getTeamActions(
        @PathVariable Long teamId,
        @RequestParam(required = false) MeetingActionStatus status,
        Authentication authentication
    );
}
