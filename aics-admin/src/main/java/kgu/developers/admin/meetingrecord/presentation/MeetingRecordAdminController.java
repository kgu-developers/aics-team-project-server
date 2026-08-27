package kgu.developers.admin.meetingrecord.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.meetingrecord.presentation.response.MeetingRecordAdminPageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AdminMeetingRecord", description = "관리자 회의록 조회 API")
public interface MeetingRecordAdminController {

    @Operation(
        summary = "담당 분반 회의록 통합 조회 API",
        description = """
            Description : 담당 교수가 맡은 전체 분반의 회의록을 최신순으로 조회한다.
                sectionId를 전달하면 해당 분반으로 범위를 좁힌다.
            Assignee : 최태양
            """
    )
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = MeetingRecordAdminPageResponse.class))
    )
    ResponseEntity<MeetingRecordAdminPageResponse> getMeetingRecords(
        @Parameter(description = "분반 필터") @RequestParam(required = false) @Positive Long sectionId,
        Pageable pageable,
        Authentication authentication
    );
}
