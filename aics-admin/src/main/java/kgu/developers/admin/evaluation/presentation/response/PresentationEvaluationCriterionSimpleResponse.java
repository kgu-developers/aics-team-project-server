package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import lombok.Builder;

@Builder
public record PresentationEvaluationCriterionSimpleResponse(
    @Schema(description = "평가 항목 식별자", example = "1", requiredMode = REQUIRED)
    Long criterionId,

    @Schema(description = "평가 항목 제목", example = "프로젝트 완성도", requiredMode = REQUIRED)
    String title,

    @Schema(description = "최대 점수", example = "10", requiredMode = REQUIRED)
    int maxScore,

    @Schema(description = "표시 순서", example = "1", requiredMode = REQUIRED)
    int displayOrder
) {
    public static PresentationEvaluationCriterionSimpleResponse from(TeamEvaluationCriterion criterion) {
        return PresentationEvaluationCriterionSimpleResponse.builder()
            .criterionId(criterion.getId())
            .title(criterion.getTitle())
            .maxScore(criterion.getMaxScore())
            .displayOrder(criterion.getDisplayOrder())
            .build();
    }
}
