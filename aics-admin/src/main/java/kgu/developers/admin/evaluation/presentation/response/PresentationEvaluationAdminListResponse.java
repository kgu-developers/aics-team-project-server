package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record PresentationEvaluationAdminListResponse(
    @Schema(description = "분반 식별자", example = "1", requiredMode = REQUIRED)
    Long sectionId,

    @Schema(description = "발표 마일스톤 식별자", example = "3", requiredMode = REQUIRED)
    Long milestoneId,

    @Schema(description = "마일스톤 제목", example = "발표 평가", requiredMode = REQUIRED)
    String milestoneTitle,

    @Schema(description = "평가 마감 일시", example = "2026-11-26T23:59:59")
    LocalDateTime closesAt,

    @Schema(description = "발표 평가 항목(기준) 목록", requiredMode = REQUIRED)
    List<PresentationEvaluationCriterionSimpleResponse> criteria,

    @Schema(description = "팀별 발표 평가 결과 요약 목록", requiredMode = REQUIRED)
    List<PresentationEvaluationAdminTeamSummaryResponse> teams
) {
}
