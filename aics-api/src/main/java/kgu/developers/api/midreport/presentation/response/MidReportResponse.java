package kgu.developers.api.midreport.presentation.response;

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
    LocalDateTime dueDate,
    @Schema(description = "문서 상태", allowableValues = {"DRAFT", "SUBMITTED", "REVISION_REQUESTED"})
    MidReportStatus status,
    String teamLeaderName,
    LocalDateTime submittedAt,
    String submittedBy,
    MidReportRevisionResponse revision,
    List<MidReportBlockResponse> blocks
) {
    public static MidReportResponse from(
        MidReport report,
        String teamLeaderName,
        String submitterName,
        Map<String, String> editorNames
    ) {
        return new MidReportResponse(
            report.getId(),
            report.getTeamId(),
            report.getTitle(),
            report.getVersion(),
            report.getDueDate(),
            report.getStatus(),
            teamLeaderName,
            report.getSubmittedAt(),
            submitterName,
            MidReportRevisionResponse.from(report.getRevision()),
            report.getBlocks().stream()
                .map(block -> MidReportBlockResponse.from(
                    block,
                    editorNames.get(block.getLastEditedBy()),
                    report.getCreatedAt()
                ))
                .toList()
        );
    }
}
