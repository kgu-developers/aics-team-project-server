package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record PresentationEvaluationAdminRowResponse(
    @Schema(description = "평가자 학번", example = "20260001", requiredMode = REQUIRED)
    String evaluatorId,

    @Schema(description = "평가자 이름", example = "박지훈", requiredMode = REQUIRED)
    String evaluatorName,

    @Schema(description = "평가자 소속 팀명", example = "OOP-01 - 2팀")
    String teamName,

    @Schema(description = "제출 완료 여부", example = "true", requiredMode = REQUIRED)
    boolean isSubmitted,

    @Schema(description = "제출 일시 (미제출 시 null)", example = "2026-11-26T15:30:00")
    LocalDateTime submittedAt,

    @Schema(description = "항목별 부여 점수 목록 (미제출 시 점수는 null)", requiredMode = REQUIRED)
    List<PresentationEvaluationCriterionScoreResponse> scores,

    @Schema(description = "부여 점수 합계 (미제출 시 null)", example = "14")
    Integer totalScore
) {
}
