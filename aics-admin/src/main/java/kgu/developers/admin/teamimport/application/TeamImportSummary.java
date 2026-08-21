package kgu.developers.admin.teamimport.application;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.admin.importcommon.RowStatus;

public record TeamImportSummary(
    @Schema(description = "전체 행 수", example = "40")
    int total,

    @Schema(description = "파일에 담긴 팀 수", example = "10")
    int teams,

    @Schema(description = "편성 예정 행 수", example = "38")
    int valid,

    @Schema(description = "이미 같은 팀에 편성되어 건너뛸 행 수", example = "2")
    int duplicate,

    @Schema(description = "오류 행 수 (0이어야 반영 가능)", example = "0")
    int invalid
) {
    public static TeamImportSummary of(List<TeamImportRow> rows) {
        return new TeamImportSummary(
            rows.size(),
            rows.stream().map(TeamImportRow::teamName).filter(name -> !name.isEmpty())
                .collect(Collectors.toSet()).size(),
            count(rows, RowStatus.VALID),
            count(rows, RowStatus.DUPLICATE),
            count(rows, RowStatus.INVALID)
        );
    }

    private static int count(List<TeamImportRow> rows, RowStatus status) {
        return (int)rows.stream().filter(row -> row.status() == status).count();
    }
}
