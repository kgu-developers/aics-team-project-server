package kgu.developers.api.teamMember.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kgu.developers.api.teamMember.presentation.response.TeamMemberContactListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "TeamMember", description = "팀원 API")
public interface TeamMemberController {

	@Operation(summary = "팀원 연락처 조회 API", description = """
			- Description : 이 API는 팀원의 이메일과 연락처를 조회합니다.
			- 분반이 정한 연락처 공개기간 안에서만 200을 응답하며, 기간 밖이면 403을 응답합니다.
			- 공개시작이 설정되지 않은 분반은 아직 공개되지 않은 것으로 봅니다.
		""")
	@ApiResponse(
		responseCode = "200",
		content = @Content(schema = @Schema(implementation = TeamMemberContactListResponse.class)))
	ResponseEntity<TeamMemberContactListResponse> getContacts(
		@Parameter(
			description = "팀 ID는 URL 경로 변수 입니다.",
			example = "1",
			required = true
		) @Positive @PathVariable Long teamId
	);
}
