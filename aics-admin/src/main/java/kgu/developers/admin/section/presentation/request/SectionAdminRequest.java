package kgu.developers.admin.section.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SectionAdminRequest(
    @Schema(description = "교수 학번", example = "202699999", requiredMode = REQUIRED)
    @NotBlank
    String professorId,

    @Schema(description = "강좌 ID", example = "1", requiredMode = REQUIRED)
    @NotNull
    Long courseId,

    @Schema(description = "과목 코드", example = "CS101", requiredMode = REQUIRED)
    @NotBlank
    String code,

    @Schema(description = "분반명", example = "01", requiredMode = REQUIRED)
    @NotBlank
    String name,

    @Schema(description = "수업시간", example = "월123", requiredMode = REQUIRED)
    @NotBlank
    String classTime,

    @Schema(description = "정원", example = "40", requiredMode = REQUIRED)
    @NotNull
    @Positive
    Integer capacity,

    @Schema(description = "연락처 공개 시작", example = "2026-03-02T00:00:00")
    LocalDateTime contactVisibleFrom,

    @Schema(description = "연락처 공개 종료", example = "2026-06-20T18:00:00")
    LocalDateTime contactVisibleUntil
) {
}
