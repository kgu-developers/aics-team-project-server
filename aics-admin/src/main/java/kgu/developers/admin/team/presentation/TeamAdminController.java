package kgu.developers.admin.team.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.team.presentation.request.TeamUnfinalizeRequest;
import kgu.developers.admin.team.presentation.response.TeamAdminDetailResponse;
import kgu.developers.admin.team.presentation.response.TeamAdminListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
			- 분반 전체를 한 트랜잭션으로 처리하며 실패하면 상태 변경과 감사 로그를 모두 롤백합니다.
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
			- 관리자만 분반의 확정된 팀을 FORMING으로 되돌립니다. 요청 본문의 취소 사유(reason)는 필수이며 앞뒤 공백 제거 후 요청 스키마의 길이 제한을 충족해야 합니다.
			- 행위자·시각·취소 사유·변경 전후 상태를 각 팀의 감사 로그에 기록합니다.
			- 취소 후 팀 이동과 역할 변경이 가능하며, 기존 팀원·문서·평가 데이터는 보존됩니다.
			- 제출·평가 시작 여부만으로는 차단하지 않지만, 분반에 완료된 최종보고서가 있으면 409를 응답합니다.
			- 완료된 최종보고서를 먼저 재개한 뒤 취소하고, 팀 구성 정정 후 다시 확정·확인·완료합니다.
			- 이미 FORMING인 팀은 변경하지 않으며 재요청해도 중복 감사 로그가 생기지 않습니다.
			- 분반 전체를 한 트랜잭션으로 처리하며 실패하면 상태 변경과 감사 로그를 모두 롤백합니다.
		""")
	@ApiResponse(responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamAdminListResponse.class)))
	ResponseEntity<TeamAdminListResponse> unfinalizeTeams(
		@Parameter(description = "분반 ID", example = "1", required = true)
		@Positive @PathVariable Long sectionId,
		@Valid @RequestBody TeamUnfinalizeRequest request,
		@Parameter(hidden = true) Authentication authentication
	);
}
