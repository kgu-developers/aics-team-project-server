package kgu.developers.admin.midreport.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.midreport.domain.MidReportStatus;
import lombok.Builder;

@Builder
public record MidReportAdminResponse(
    @Schema(description = "중간보고서 식별자", example = "1")
    Long id,

    @Schema(description = "팀 식별자", example = "10")
    Long teamId,

    @Schema(description = "팀명", example = "A팀")
    String teamName,

    @Schema(description = "마일스톤 식별자", example = "3")
    Long milestoneId,

    @Schema(description = "중간보고서 제목", example = "CineFlow 중간보고서")
    String title,

    @Schema(description = "문서 버전", example = "2")
    Long version,

    @Schema(description = "마감 일시")
    LocalDateTime dueDate,

    @Schema(description = "보고서 제출 상태", example = "SUBMITTED")
    MidReportStatus status,

    @Schema(description = "최종 제출 일시")
    LocalDateTime submittedAt,

    @Schema(description = "최종 제출자 학번", example = "202412345")
    String submittedBy,

    @Schema(description = "최종 제출자 이름", example = "홍길동")
    String submittedByName,

    @Schema(description = "팀장 이름", example = "이팀장")
    String leaderName,

    @Schema(description = "수정 요청 이력")
    MidReportRevisionAdminResponse revision,

    @Schema(description = "작성 블록 목록")
    List<MidReportBlockAdminResponse> blocks
) {
}
