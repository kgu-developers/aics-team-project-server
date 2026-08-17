package kgu.developers.admin.team.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminKickoffResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

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
		) @Positive @PathVariable Long sectionId
	);

	@Operation(summary = "킥오프 정보 조회 API", description = """
			- Description : 이 API는 지정된 팀의 팀 운영규칙과 정기 회의일정을 조회합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamAdminKickoffResponse.class)))
	ResponseEntity<TeamAdminKickoffResponse> getKickoffByTeamId(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId
	);

	@Operation(summary = "킥오프 정보 저장 API", description = """
			- Description : 이 API는 팀명/주제/운영방식/회의방식과 팀장, 역할분담을 저장합니다.
			- 팀장은 한 명만 지정되며 기존 팀장은 자동으로 해제됩니다.
			- 요청에 없는 팀원의 역할분담은 유지됩니다.
			- 확정된 팀은 수정할 수 없어 409를 응답합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamAdminKickoffResponse.class)))
	ResponseEntity<TeamAdminKickoffResponse> updateKickoff(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId,
		@Parameter(
			description = "킥오프 정보 request 객체 입니다.",
			required = true
		) @Valid @RequestBody TeamKickoffUpdateRequest request
	);
}
