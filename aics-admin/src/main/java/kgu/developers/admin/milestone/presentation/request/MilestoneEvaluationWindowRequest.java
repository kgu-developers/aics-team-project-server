package kgu.developers.admin.milestone.presentation.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

@Schema(description = "마일스톤 평가 기간 수정 요청")
public record MilestoneEvaluationWindowRequest(
        @Schema(description = "평가 시작 시각", example = "2026-09-13T00:00:00")
        LocalDateTime evaluationOpensAt,

        @Schema(description = "평가 종료 시각", example = "2026-09-15T23:59:59")
        LocalDateTime evaluationClosesAt,

        @Schema(description = "기존 평가 기간 해제 여부", example = "false")
        boolean clearEvaluationWindow
) {

    @AssertTrue(message = "평가 기간을 입력하거나 해제 여부를 명시해야 합니다.")
    @Schema(hidden = true)
    public boolean isEvaluationWindowIntentValid() {
        boolean hasEvaluationWindow = evaluationOpensAt != null || evaluationClosesAt != null;
        return clearEvaluationWindow ? !hasEvaluationWindow : hasEvaluationWindow;
    }
}
