package kgu.developers.admin.meetingrecord.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingActionAdminPageResponse;
import kgu.developers.domain.meetingrecord.domain.MeetingActionStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AdminMeetingAction", description = "관리자 액션플랜 조회 API")
public interface MeetingActionAdminController {

    @Operation(
        summary = "담당 분반 액션플랜 통합 조회 API",
        description = """
            Description : 담당 교수가 맡은 분반의 모든 팀 액션플랜을 생성 최신순으로 조회한다.
                teamId를 전달하면 해당 팀 하나로 범위를 좁힌다.
                meetingRecordId를 전달하면 해당 회의록의 액션플랜만 조회한다.
                status를 전달하면 해당 상태(TODO/IN_PROGRESS/DONE)만 조회한다.
                담당 분반이 아니거나 존재하지 않는 분반·팀·회의록을 지정하면 모두 403으로 응답한다.
                정렬은 생성 최신순으로 고정이며 정렬 파라미터는 지원하지 않는다.
            Assignee : 황호찬
            """
    )
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = MeetingActionAdminPageResponse.class))
    )
    @ApiResponse(responseCode = "403", description = "담당 분반이 아닌 경우", content = @Content)
    ResponseEntity<MeetingActionAdminPageResponse> getSectionMeetingActions(
        @Parameter(description = "분반 식별자") @PathVariable @Positive Long sectionId,
        @Parameter(description = "팀 필터") @RequestParam(required = false) @Positive Long teamId,
        @Parameter(description = "회의록 필터") @RequestParam(required = false) @Positive Long meetingRecordId,
        @Parameter(description = "상태 필터(TODO, IN_PROGRESS, DONE)")
        @RequestParam(required = false) MeetingActionStatus status,
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @Parameter(description = "페이지 크기(최대 100)", example = "20")
        @RequestParam(defaultValue = "20") @Positive @Max(100) int size,
        Authentication authentication
    );
}
