package kgu.developers.admin.evaluation.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfessorPresentationScoreResponse(
    @Schema(description = "평가 항목 식별자") Long criterionId,
    @Schema(description = "평가 항목명") String title,
    @Schema(description = "최대 점수") int maxScore,
    @Schema(description = "교수자 점수. 미평가 시 null") Integer score
) {
}
