package kgu.developers.api.auditlog.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.auditlog.presentation.response.TeamActivitySummaryResponse;
import kgu.developers.api.auditlog.presentation.response.TeamHistoryPageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "AuditLog", description = "팀 감사 로그 API")
public interface AuditLogController {

    @Operation(
            summary = "팀 변경 이력 조회 API",
            description = """
                    Description : 해당 팀을 대상으로 기록된 AuditLog를 최신순으로 조회합니다.
                    eventType과 metadata는 프론트에서 표시 문구로 변환합니다.
                    targetType : TEAM
                    Assignee : 담당자명
                    """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TeamHistoryPageResponse.class)))
    ResponseEntity<TeamHistoryPageResponse> getTeamHistories(
            @Positive @PathVariable Long teamId,
            Pageable pageable,
            Authentication authentication
    );

    @Operation(
            summary = "팀원 활동 요약 조회 API",
            description = """
                    Description : 팀원별 마지막 로그인과 마지막 활동만 요약하여 조회합니다.
                    활동 이력이 없는 팀원의 lastActivity는 null입니다.
                    targetType : TEAM
                    Assignee : 담당자명
                    """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TeamActivitySummaryResponse.class)))
    ResponseEntity<TeamActivitySummaryResponse> getTeamActivitySummary(
            @Positive @PathVariable Long teamId,
            Authentication authentication
    );
}
