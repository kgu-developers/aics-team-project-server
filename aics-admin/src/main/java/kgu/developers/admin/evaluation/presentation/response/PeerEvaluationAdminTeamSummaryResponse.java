package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PeerEvaluationAdminTeamSummaryResponse(
    @Schema(description = "팀 식별자", example = "1", requiredMode = REQUIRED)
    Long teamId,

    @Schema(description = "팀명", example = "OOP-01 - 1팀", requiredMode = REQUIRED)
    String teamName,

    @Schema(description = "제출 완료한 팀원 수", example = "1", requiredMode = REQUIRED)
    int submittedCount,

    @Schema(description = "전체 활성 팀원 수", example = "2", requiredMode = REQUIRED)
    int totalMemberCount,

    @Schema(description = "최근 제출 일시 (제출 내역이 없으면 null)", example = "2026-12-14T15:30:00")
    LocalDateTime lastSubmittedAt,

    @Schema(description = "팀 회의록 수", example = "1", requiredMode = REQUIRED)
    long meetingRecordCount
) {
}
