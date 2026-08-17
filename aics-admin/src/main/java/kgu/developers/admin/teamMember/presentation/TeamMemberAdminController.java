package kgu.developers.admin.teamMember.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.teamMember.presentation.request.TeamMemberUpdateRequest;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberAdminResponse;
import kgu.developers.admin.teamMember.presentation.response.TeamMemberContactAdminListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "TeamMember", description = "팀원 관리 API")
public interface TeamMemberAdminController {

	@Operation(summary = "팀원 이동/역할 변경 API", description = """
			- Description : 이 API는 팀원의 소속 팀과 역할을 수정합니다.
			- 값이 없는 필드는 변경하지 않습니다.
			- 옮길 팀에 해당 학생이 이미 있으면 409, 팀장이 이미 있는 팀으로 팀장을 옮기면 409를 응답합니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamMemberAdminResponse.class)))
	ResponseEntity<TeamMemberAdminResponse> updateTeamMember(
		@Parameter(
			description = "현재 소속 팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId,
		@Parameter(
			description = "학번은 URL 경로 변수 입니다.",
			example = "202699999",
			required = true
		) @PathVariable String studentNumber,
		@Parameter(
			description = "팀원 수정 request 객체 입니다.",
			required = true
		) @Valid @RequestBody TeamMemberUpdateRequest request
	);

	@Operation(summary = "팀원 연락처 조회 API", description = """
			- Description : 이 API는 팀원의 이메일과 연락처를 조회합니다.
			- 분반이 정한 연락처 공개기간 안에서만 200을 응답하며, 기간 밖이면 403을 응답합니다.
			- 공개시작이 설정되지 않은 분반은 아직 공개되지 않은 것으로 봅니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamMemberContactAdminListResponse.class)))
	ResponseEntity<TeamMemberContactAdminListResponse> getContacts(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId
	);
}
