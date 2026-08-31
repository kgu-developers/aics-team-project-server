package kgu.developers.admin.teammessage.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.teammessage.presentation.response.TeamMessageAdminPageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AdminTeamMessage", description = "관리자 통합 쪽지함 API")
public interface TeamMessageAdminController {

    @Operation(
        summary = "담당 분반 통합 쪽지함 조회 API",
        description = """
            Description : 담당 교수가 맡은 전체 분반의 팀 메시지를 최신순으로 조회한다.
                sectionId를 전달하면 해당 분반으로 범위를 좁히며, 관리자 기준 읽음 여부와 전체 미확인 수를 반환한다.
                현재 권한 모델에서 ADMIN은 교수 계정이며, 조교는 USER의 Enrollment.role로 구분한다.
                호출자의 인증 식별자를 교수 학번으로 사용하므로 본인이 담당하는 분반만 조회할 수 있다.
            Assignee : 최태양
            """
    )
    @ApiResponse(
        responseCode = "200",
        content = @Content(schema = @Schema(implementation = TeamMessageAdminPageResponse.class))
    )
    ResponseEntity<TeamMessageAdminPageResponse> getMessages(
        @Parameter(description = "분반 필터") @RequestParam(required = false) @Positive Long sectionId,
        Pageable pageable,
        Authentication authentication
    );
}
