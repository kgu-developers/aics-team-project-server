package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record PeerEvaluationAdminTeamDetailResponse(
    @Schema(description = "팀 식별자", example = "1", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "OOP-01 - 1팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "상호평가 양식 식별자", example = "10", requiredMode = REQUIRED)
    Long formId,

    @Schema(description = "평가 마감 일시 (양식이 없으면 null)", example = "2026-12-15T23:59:59")
    LocalDateTime closesAt,

    @Schema(description = "팀원 목록", requiredMode = REQUIRED)
    List<PeerEvaluationMemberResponse> members,

    @Schema(description = "팀원별 평가 매트릭스 및 서술형 평가 행 목록", requiredMode = REQUIRED)
    List<PeerEvaluationRowResponse> evaluations,

    @Schema(description = "팀 회의록 목록", requiredMode = REQUIRED)
    List<PeerEvaluationMeetingRecordSummaryResponse> meetingRecords
) {
}
