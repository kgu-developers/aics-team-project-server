package kgu.developers.admin.milestone.presentation.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마일스톤 평가 기간 수정 요청. 두 값을 모두 null로 보내면 평가 기간을 해제합니다.")
public record MilestoneEvaluationWindowRequest(
        @Schema(description = "평가 시작 시각", example = "2026-09-13T00:00:00")
        LocalDateTime evaluationOpensAt,

        @Schema(description = "평가 종료 시각", example = "2026-09-15T23:59:59")
        LocalDateTime evaluationClosesAt
) {
}
