package kgu.developers.api.teamimport.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamImportApplyResponse(
    @Schema(description = "업로드 ID", example = "1", requiredMode = REQUIRED)
    Long importId,

    @Schema(description = "새로 만든 팀 수", example = "10", requiredMode = REQUIRED)
    int createdTeams,

    @Schema(description = "편성된 팀원 수", example = "38", requiredMode = REQUIRED)
    int appliedMembers,

    @Schema(description = "이미 편성되어 건너뛴 수", example = "2", requiredMode = REQUIRED)
    int skipped
) {
}
