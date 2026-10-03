package kgu.developers.admin.sectionartifact.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import kgu.developers.domain.submission.application.query.SectionArtifactTeamRow;

@Builder
public record SectionArtifactSummaryAdminResponse(
        @Schema(description = "팀 식별자", example = "20", requiredMode = REQUIRED)
        Long teamId,

        @Schema(description = "팀명", example = "1팀", requiredMode = REQUIRED)
        String teamName,

        @Schema(description = "팀원 목록", requiredMode = REQUIRED)
        List<SectionArtifactMemberAdminResponse> members,

        @Schema(description = "기준일까지 작성된 회의록 수", example = "8", requiredMode = REQUIRED)
        long meetingRecordCount,

        @Schema(description = "회의록 수정 로그 수", example = "3", requiredMode = REQUIRED)
        long meetingRecordEditCount,

        @Schema(description = "제출 이력이 1건 이상 있는 단계 수", example = "4", requiredMode = REQUIRED)
        long submittedStageCount,

        @Schema(description = "마감이 지났는데 제출 이력이 없는 단계 수", example = "1", requiredMode = REQUIRED)
        long overdueMissingStageCount
) {

    public static SectionArtifactSummaryAdminResponse from(SectionArtifactTeamRow row) {
        return SectionArtifactSummaryAdminResponse.builder()
                .teamId(row.teamId())
                .teamName(row.teamName())
                .members(row.members().stream()
                        .map(SectionArtifactMemberAdminResponse::from)
                        .toList())
                .meetingRecordCount(row.meetingRecordCount())
                .meetingRecordEditCount(row.meetingRecordEditCount())
                .submittedStageCount(row.submittedStageCount())
                .overdueMissingStageCount(row.overdueMissingStageCount())
                .build();
    }
}
