package kgu.developers.admin.teamimport.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.admin.teamimport.application.TeamImportRow;
import kgu.developers.admin.teamimport.application.TeamImportSummary;

public record TeamImportPreviewResponse(
    @Schema(description = "업로드 ID (반영 API에 사용)", example = "1", requiredMode = REQUIRED)
    Long importId,

    @Schema(description = "검증 요약", requiredMode = REQUIRED)
    TeamImportSummary summary,

    @Schema(description = "행별 검증 결과", requiredMode = REQUIRED)
    List<TeamImportRow> rows
) {
}
