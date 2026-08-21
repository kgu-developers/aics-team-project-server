package kgu.developers.api.enrollmentimport.application;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.api.importcommon.RowStatus;

public record EnrollmentImportSummary(
    @Schema(description = "전체 행 수", example = "43")
    int total,

    @Schema(description = "이미 가입된 학생 중 등록 예정 행 수", example = "1")
    int valid,

    @Schema(description = "계정 생성 후 등록 예정 행 수", example = "41")
    int newUser,

    @Schema(description = "이미 등록되어 건너뛸 행 수", example = "1")
    int duplicate,

    @Schema(description = "오류 행 수 (0이어야 반영 가능)", example = "0")
    int invalid
) {
    public static EnrollmentImportSummary of(List<EnrollmentImportRow> rows) {
        return new EnrollmentImportSummary(
            rows.size(),
            count(rows, RowStatus.VALID),
            count(rows, RowStatus.NEW_USER),
            count(rows, RowStatus.DUPLICATE),
            count(rows, RowStatus.INVALID)
        );
    }

    private static int count(List<EnrollmentImportRow> rows, RowStatus status) {
        return (int)rows.stream().filter(row -> row.status() == status).count();
    }
}
