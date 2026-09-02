package kgu.developers.api.teamMember.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.user.domain.User;

public record TeamMemberContactResponse(
	@Schema(description = "학번", example = "202699999", requiredMode = REQUIRED)
	String studentNumber,

	@Schema(description = "이름", example = "김철수")
	String name,

	@Schema(description = "이메일", example = "kim@kgu.ac.kr")
	String email,

	@Schema(description = "연락처", example = "010-0000-0000")
	String phone,

	@Schema(description = "팀장 여부", example = "true", requiredMode = REQUIRED)
	boolean isLeader
) {

	public static TeamMemberContactResponse of(TeamMember teamMember, User user) {
		return new TeamMemberContactResponse(
			teamMember.getUserId(),
			user == null ? null : user.getName(),
			user == null ? null : user.getEmail(),
			user == null ? null : user.getPhone(),
			teamMember.isLeader()
		);
	}
}
