package kgu.developers.api.midreport.presentation.response;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportStatus;

public record MidReportResponse(
    Long id,
    Long teamId,
    String title,
    Long version,
    @Schema(description = "연결된 마일스톤의 현재 제출 마감일")
    LocalDateTime dueDate,
    @Schema(description = "문서 상태", allowableValues = {"DRAFT", "SUBMITTED", "REVISION_REQUESTED"})
    MidReportStatus status,
    String teamLeaderName,
    LocalDateTime submittedAt,
    @Schema(description = "최종 제출자 학번", nullable = true)
    String submittedBy,
    @Schema(description = "최종 제출자 이름", nullable = true)
    String submittedByName,
    MidReportRevisionResponse revision,
    List<MidReportBlockResponse> blocks
) {
    public static MidReportResponse from(
        MidReport report,
        LocalDateTime currentDueDate,
        String teamLeaderName,
        String submitterName,
        Map<String, String> editorNames,
        Map<String, JsonNode> resolvedFields
    ) {
        return new MidReportResponse(
            report.getId(),
            report.getTeamId(),
            report.getTitle(),
            report.getVersion(),
            currentDueDate,
            report.getStatus(),
            teamLeaderName,
            report.getSubmittedAt(),
            report.getSubmittedBy(),
            submitterName,
            MidReportRevisionResponse.from(report.getRevision()),
            report.getBlocks().stream()
                .map(block -> MidReportBlockResponse.from(
                    block,
                    editorNames.get(block.getLastEditedBy()),
                    report.getCreatedAt(),
                    resolvedFields.getOrDefault(block.getKey(), block.getFields())
                ))
                .toList()
        );
    }
}
