package kgu.developers.api.teamMember.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamMemberContactListResponse(
	@Schema(description = "팀원 연락처 리스트", requiredMode = REQUIRED)
	List<TeamMemberContactResponse> contents
) {
}
