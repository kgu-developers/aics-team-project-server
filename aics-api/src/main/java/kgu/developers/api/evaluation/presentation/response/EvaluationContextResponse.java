package kgu.developers.api.evaluation.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EvaluationContextResponse(
    @Schema(description = "발표 평가에 사용하는 PRESENTATION 마일스톤 ID. 설정 전이면 null", example = "15")
    String presentationMilestoneId,
    @Schema(description = "상호평가 양식 ID. 설정 전이거나 공개 전이면 null", example = "3")
    String peerEvaluationFormId
) {
    public static EvaluationContextResponse of(Long presentationMilestoneId, Long peerEvaluationFormId) {
        return new EvaluationContextResponse(asString(presentationMilestoneId), asString(peerEvaluationFormId));
    }

    private static String asString(Long id) {
        return id == null ? null : id.toString();
    }
}
