package kgu.developers.admin.enrollmentimport.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record EnrollmentImportApplyResponse(
    @Schema(description = "업로드 ID", example = "1", requiredMode = REQUIRED)
    Long importId,

    @Schema(description = "수강 등록된 수", example = "42", requiredMode = REQUIRED)
    int applied,

    @Schema(description = "새로 가입시킨 계정 수 (초기 비밀번호는 학번)", example = "41", requiredMode = REQUIRED)
    int createdUsers,

    @Schema(description = "이미 등록되어 건너뛴 수", example = "1", requiredMode = REQUIRED)
    int skipped
) {
}
