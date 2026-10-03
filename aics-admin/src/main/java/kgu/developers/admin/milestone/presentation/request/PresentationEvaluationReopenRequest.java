package kgu.developers.admin.milestone.presentation.request;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PresentationEvaluationReopenRequest(
    @Schema(description = "새 평가 종료 시각. 현재보다 늦어야 합니다.", example = "2026-10-02T18:00:00")
    @NotNull LocalDateTime evaluationClosesAt
) {
}
