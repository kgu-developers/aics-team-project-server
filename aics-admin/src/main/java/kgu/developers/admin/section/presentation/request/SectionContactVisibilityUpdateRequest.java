package kgu.developers.admin.section.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SectionContactVisibilityUpdateRequest(
    @Schema(description = "연락처 공개 시작", example = "2026-03-02T00:00:00")
    LocalDateTime visibleFrom,

    @Schema(description = "연락처 공개 종료", example = "2026-06-20T18:00:00")
    LocalDateTime visibleUntil
    ) {
}
