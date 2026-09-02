package kgu.developers.admin.teamMember.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** PATCH 요청이라 null인 필드는 변경하지 않는다. */
public record TeamMemberUpdateRequest(
	@Schema(description = "이동할 팀 ID. 값이 없으면 팀을 옮기지 않습니다.", example = "2")
	@Positive
	Long targetTeamId,

	@Schema(description = "프로젝트 역할. 값이 없으면 유지합니다.", example = "백엔드")
	@Size(max = 50)
	String projectRole,

	@Schema(description = "팀장 여부. 값이 없으면 유지합니다.", example = "true")
	Boolean isLeader
) {
}
