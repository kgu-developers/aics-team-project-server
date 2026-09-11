package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record PresentationEvaluationAdminTeamSummaryResponse(
    @Schema(description = "팀 식별자", example = "1", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "OOP-01 - 1팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "프로젝트 주제", example = "AI 기반 팀 프로젝트 관리 서비스")
    String projectTitle,

    @Schema(description = "해당 팀에 대한 발표 평가 완료 건수", example = "2", requiredMode = REQUIRED)
    int evaluationCount,

    @Schema(description = "평가 항목별 평균 점수 목록", requiredMode = REQUIRED)
    List<PresentationEvaluationCriterionScoreResponse> scores,

    @Schema(description = "평가 항목별 평균 점수의 합계 (평가 데이터가 없으면 null)", example = "14.0")
    Double totalScore
) {
}
