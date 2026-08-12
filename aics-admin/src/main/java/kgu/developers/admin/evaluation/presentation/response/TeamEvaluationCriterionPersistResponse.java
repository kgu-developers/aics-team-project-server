package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record TeamEvaluationCriterionPersistResponse(
    @Schema(description = "생성된 평가 항목 ID", example = "1", requiredMode = REQUIRED)
    Long id
) {
  public static TeamEvaluationCriterionPersistResponse of(Long id) {
    return new TeamEvaluationCriterionPersistResponse(id);
  }
}
