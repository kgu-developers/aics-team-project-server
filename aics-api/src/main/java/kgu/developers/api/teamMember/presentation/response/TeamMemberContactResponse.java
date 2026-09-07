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

	@Schema(description = "연락처. 기준 데이터는 계정 정보(User)이며, 팀명단 업로드로 올린 연락처도 "
		+ "여기에 반영된다", example = "010-0000-0000")
	String phone,

	@Schema(description = "학년. 팀이 아닌 분반 수강 정보(Enrollment) 기준이라 팀을 옮겨도 유지된다", example = "3")
	String grade,

	@Schema(description = "팀장 여부", example = "true", requiredMode = REQUIRED)
	boolean isLeader
) {

	public static TeamMemberContactResponse of(TeamMember teamMember, User user, String grade) {
		return new TeamMemberContactResponse(
			teamMember.getUserId(),
			user == null ? null : user.getName(),
			user == null ? null : user.getEmail(),
			user == null ? null : user.getPhone(),
			grade,
			teamMember.isLeader()
		);
	}
}
