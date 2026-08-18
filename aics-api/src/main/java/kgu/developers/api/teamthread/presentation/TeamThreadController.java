package kgu.developers.api.teamthread.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kgu.developers.api.teamthread.presentation.response.TeamThreadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "TeamThread", description = "팀 커뮤니케이션 스레드 API")
public interface TeamThreadController {

    @Operation(
        summary = "팀 스레드 조회 API",
        description = """
            Description : 팀의 교수-팀 커뮤니케이션 스레드(공식 히스토리)를 조회한다.
                아직 스레드가 없는 팀이라면 새로 생성하여 반환한다 (team_id UNIQUE 제약).
            Assignee : 담당자명
            """
    )
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TeamThreadResponse.class)))
    ResponseEntity<TeamThreadResponse> getThread(@PathVariable Long teamId, Authentication authentication);
}
