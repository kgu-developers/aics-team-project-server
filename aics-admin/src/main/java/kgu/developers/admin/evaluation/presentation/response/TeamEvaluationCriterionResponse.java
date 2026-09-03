package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;

public record TeamEvaluationCriterionResponse(
    @Schema(description = "평가 항목 ID", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "평가 항목명", example = "객체지향 설계", requiredMode = REQUIRED)
    String title,

    @Schema(description = "항목 최대 점수", example = "30", requiredMode = REQUIRED)
    int maxScore,

    @Schema(description = "표시 순서", example = "0", requiredMode = REQUIRED)
    int displayOrder
) {
  public static TeamEvaluationCriterionResponse from(TeamEvaluationCriterion criterion) {
    return new TeamEvaluationCriterionResponse(
        criterion.getId(),
        criterion.getTitle(),
        criterion.getMaxScore(),
        criterion.getDisplayOrder());
  }
}
