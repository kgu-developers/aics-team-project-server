package kgu.developers.admin.importstatus.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.importBatch.domain.ImportBatch;

public record RosterImportAppliedResponse(

    @Schema(description = "마지막으로 반영한 파일명. 기존 반영 건은 저장된 파일명이 없어 null일 수 있습니다.",
        example = "2026-2-A반-학생명단.xlsx", nullable = true, requiredMode = REQUIRED)
    String fileName,

    @Schema(description = "마지막 반영 시각", example = "2026-09-09T14:30:00", requiredMode = REQUIRED)
    LocalDateTime appliedAt
) {
    public static RosterImportAppliedResponse from(ImportBatch batch) {
        return new RosterImportAppliedResponse(batch.getFileName(), batch.getUpdatedAt());
    }
}
