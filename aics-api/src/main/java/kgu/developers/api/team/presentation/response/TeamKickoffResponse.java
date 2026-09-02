package kgu.developers.api.team.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.api.teamMember.presentation.response.TeamMemberResponse;
import kgu.developers.domain.team.domain.Team;

public record TeamKickoffResponse(
	@Schema(description = "팀 ID", example = "1", requiredMode = REQUIRED)
	Long id,

	@Schema(description = "팀명", example = "1팀", requiredMode = REQUIRED)
	String name,

	@Schema(description = "팀 운영방식", example = "매주 화요일 회고")
	String kickoffRule,

	@Schema(description = "회의방식", example = "매주 목 19:00 온라인")
	String meetingSchedule,

	@Schema(description = "팀원 목록. 팀장 여부와 역할분담이 담깁니다.", requiredMode = REQUIRED)
	List<TeamMemberResponse> members
) {

	public static TeamKickoffResponse of(Team team, List<TeamMemberResponse> members) {
		return new TeamKickoffResponse(
			team.getId(),
			team.getName(),
			team.getKickoffRule(),
			team.getMeetingSchedule(),
			members
		);
	}
}
