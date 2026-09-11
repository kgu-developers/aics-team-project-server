package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record PresentationEvaluationAdminTeamDetailResponse(
    @Schema(description = "팀 식별자", example = "1", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "OOP-01 - 1팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "프로젝트 주제", example = "AI 기반 팀 프로젝트 관리 서비스")
    String projectTitle,

    @Schema(description = "마일스톤 식별자", example = "3", requiredMode = REQUIRED)
    Long milestoneId,

    @Schema(description = "평가 마감 일시", example = "2026-11-26T23:59:59")
    LocalDateTime closesAt,

    @Schema(description = "발표 평가 항목(기준) 목록", requiredMode = REQUIRED)
    List<PresentationEvaluationCriterionSimpleResponse> criteria,

    @Schema(description = "평가자별 점수 목록", requiredMode = REQUIRED)
    List<PresentationEvaluationAdminRowResponse> evaluations,

    @Schema(description = "팀 회의록 목록", requiredMode = REQUIRED)
    List<PresentationMeetingRecordResponse> meetingRecords
) {
}
