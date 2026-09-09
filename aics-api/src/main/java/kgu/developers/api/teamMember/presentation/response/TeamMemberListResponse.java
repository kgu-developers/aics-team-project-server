package kgu.developers.api.teamMember.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TeamMemberListResponse(
	@Schema(description = "팀원 리스트", requiredMode = REQUIRED)
	List<TeamMemberResponse> contents
) {
}
