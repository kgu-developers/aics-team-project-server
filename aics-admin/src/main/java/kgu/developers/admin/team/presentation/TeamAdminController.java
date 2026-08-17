package kgu.developers.admin.team.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Team", description = "팀 관리 API")
public interface TeamAdminController {

	@Operation(summary = "팀 상세 조회 API", description = """
			- Description : 이 API는 지정된 팀의 상세 정보를 조회합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamAdminDetailResponse.class)))
	ResponseEntity<TeamAdminDetailResponse> getTeamById(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId
	);
}
