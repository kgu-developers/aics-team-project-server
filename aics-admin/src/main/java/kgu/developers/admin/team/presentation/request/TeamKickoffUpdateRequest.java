package kgu.developers.admin.team.presentation.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT 요청이라 킥오프 정보를 통째로 덮어쓴다. */
public record TeamKickoffUpdateRequest(
	@Schema(description = "팀명", example = "1팀", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 200)
	String name,

	@Schema(description = "프로젝트 주제", example = "AI 학습 도우미")
	@Size(max = 200)
	String topic,

	@Schema(description = "팀 운영방식", example = "매주 화요일 회고", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	String kickoffRule,

	@Schema(description = "회의방식", example = "매주 목 19:00 온라인", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	String meetingSchedule,

	@Schema(description = "팀장 학번", example = "202699999", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	String leaderStudentNumber,

	@Schema(description = "역할분담. 넘기지 않은 팀원의 역할은 유지됩니다.")
	@Valid
	List<MemberRole> memberRoles
) {

	public record MemberRole(
		@Schema(description = "학번", example = "202699999", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank
		String studentNumber,

		@Schema(description = "프로젝트 역할", example = "백엔드")
		@Size(max = 50)
		String projectRole
	) {
	}
}
