package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PresentationEvaluationCriterionScoreResponse(
    @Schema(description = "평가 항목 식별자", example = "1", requiredMode = REQUIRED)
    Long criterionId,

    @Schema(description = "평가 항목 제목", example = "프로젝트 완성도", requiredMode = REQUIRED)
    String criterionTitle,

    @Schema(description = "평균 점수(요약) 또는 부여 점수(상세). 미제출 또는 평가 데이터가 없으면 null", example = "5.0")
    Double score
) {
    public static PresentationEvaluationCriterionScoreResponse of(Long criterionId, String criterionTitle, Double score) {
        return PresentationEvaluationCriterionScoreResponse.builder()
            .criterionId(criterionId)
            .criterionTitle(criterionTitle)
            .score(score)
            .build();
    }
}
