package kgu.developers.api.teamMember.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.domain.User;

public record TeamMemberResponse(
		@Schema(description = "팀원 ID", example = "1", requiredMode = REQUIRED)
		Long id,

		@Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
		String studentNumber,

		@Schema(description = "이름", example = "김철수")
		String name,

		@Schema(description = "팀장 여부", example = "true", requiredMode = REQUIRED)
		boolean isLeader,

		@Schema(description = "프로젝트 역할", example = "백엔드")
		String projectRole
) {

	public static TeamMemberResponse of(TeamMember teamMember, User user) {
		return new TeamMemberResponse(
				teamMember.getId(),
				teamMember.getUserId(),
				user == null ? null : user.getName(),
				teamMember.isLeader(),
				teamMember.getProjectRole()
		);
	}
}
