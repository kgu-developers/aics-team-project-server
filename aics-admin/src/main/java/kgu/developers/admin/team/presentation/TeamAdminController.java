package kgu.developers.admin.team.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

	@Operation(summary = "팀 배정 확정 API", description = """
			- Description : 이 API는 분반의 팀 배정을 최종 확정합니다.
			- 확정된 팀의 팀원은 이동/역할 변경이 불가하며 409를 응답합니다.
			- 이미 확정된 팀이 있어도 그대로 두므로 여러 번 호출해도 결과는 같습니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamAdminListResponse.class)))
	ResponseEntity<TeamAdminListResponse> finalizeTeams(
		@Parameter(
			description = "분반 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long sectionId,
		@Parameter(hidden = true) Authentication authentication
	);

	@Operation(summary = "팀 배정 확정 취소 API", description = """
			- 관리자만 분반의 확정된 팀을 FORMING으로 되돌립니다. 취소 사유와 요청 본문은 없습니다.
			- 취소 후 팀 이동과 역할 변경이 가능하며, 기존 팀원·문서·평가 데이터는 보존됩니다.
			- 평가 시작·제출 여부와 관계없이 취소할 수 있습니다.
			- 이미 FORMING인 팀은 변경하지 않으며 재요청해도 중복 감사 로그가 생기지 않습니다.
			- 팀별 트랜잭션으로 처리하므로 실패한 팀은 롤백되고 앞서 처리된 팀은 유지됩니다.
		""")
	@ApiResponse(responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamAdminListResponse.class)))
	ResponseEntity<TeamAdminListResponse> unfinalizeTeams(
		@Parameter(description = "분반 ID", example = "1", required = true)
		@Positive @PathVariable Long sectionId,
		@Parameter(hidden = true) Authentication authentication
	);
}
