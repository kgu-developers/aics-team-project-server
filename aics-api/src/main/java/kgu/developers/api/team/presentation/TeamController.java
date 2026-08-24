package kgu.developers.api.team.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.team.presentation.request.TeamKickoffUpdateRequest;
import kgu.developers.api.team.presentation.response.TeamKickoffResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Team", description = "팀 API")
public interface TeamController {

	@Operation(summary = "킥오프 정보 조회 API", description = """
			- Description : 이 API는 지정된 팀의 팀 운영규칙과 정기 회의일정을 조회합니다.
			- 해당 팀 소속 팀원 또는 담당 교수만 조회할 수 있으며, 그 외에는 403을 응답합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamKickoffResponse.class)))
	ResponseEntity<TeamKickoffResponse> getKickoffByTeamId(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId,
		Authentication authentication
	);

	@Operation(summary = "킥오프 정보 저장 API", description = """
			- Description : 이 API는 팀명/운영방식/회의방식과 팀장, 역할분담을 저장합니다.
			- 해당 팀 소속 팀원만 저장할 수 있으며, 그 외에는 403을 응답합니다.
			- 팀장은 한 명만 지정되며 기존 팀장은 자동으로 해제됩니다.
			- 요청에 없는 팀원의 역할분담은 유지됩니다.
			- 확정된 팀은 수정할 수 없어 409를 응답합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamKickoffResponse.class)))
	ResponseEntity<TeamKickoffResponse> updateKickoff(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId,
		@Parameter(
			description = "킥오프 정보 request 객체 입니다.",
			required = true
		) @Valid @RequestBody TeamKickoffUpdateRequest request,
		Authentication authentication
	);

	@Operation(summary = "팀장 자진 선언 API", description = """
			- Description : 해당 팀의 팀원이 스스로 팀장으로 선언합니다.
			- 팀장이 아직 없는 경우에만 선언할 수 있으며, 최초 선언자가 팀장과 팀 확정을 함께 완료합니다.
			- 이미 팀장이 있거나 확정된 팀이면 409를 응답합니다.
			- 해당 팀 소속 팀원만 선언할 수 있으며, 그 외에는 403을 응답합니다.
		""")
	@ApiResponse(responseCode = "204")
	ResponseEntity<Void> claimLeader(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId,
		Authentication authentication
	);
}
