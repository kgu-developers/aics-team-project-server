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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "TeamMember", description = "팀원 관리 API")
public interface TeamMemberAdminController {

	@Operation(summary = "팀원 이동/역할 변경 API", description = """
			- Description : 이 API는 팀원의 소속 팀과 역할을 수정합니다.
			- 값이 없는 필드는 변경하지 않습니다.
			- 옮길 팀에 해당 학생이 이미 있으면 409, 팀장이 이미 있는 팀으로 팀장을 옮기면 409를 응답합니다.
			- 같은 팀 안에서 팀장을 바꾸는 경우에는 기존 팀장이 자동으로 해제됩니다.
			- 다른 분반의 팀으로는 옮길 수 없어 400을 응답합니다.
			- 확정된 팀은 팀장 여부(isLeader)만 변경할 수 있습니다.
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
}
