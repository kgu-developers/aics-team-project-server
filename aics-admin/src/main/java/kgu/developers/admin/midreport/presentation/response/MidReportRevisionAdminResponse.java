package kgu.developers.admin.midreport.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.midreport.domain.MidReportRevision;

public record MidReportRevisionAdminResponse(
    @Schema(description = "수정 요청된 블록 키 목록")
    List<String> affectedBlockKeys,

    @Schema(description = "수정 반영된 블록 키 목록")
    List<String> changedBlockKeys,

    @Schema(description = "수정 요청 일시")
    LocalDateTime requestedAt,

    @Schema(description = "재제출 일시")
    LocalDateTime resubmittedAt
) {
    public static MidReportRevisionAdminResponse from(MidReportRevision revision) {
        if (revision == null) {
            return null;
        }
        return new MidReportRevisionAdminResponse(
            revision.affectedBlockKeys(),
            revision.changedBlockKeys(),
            revision.requestedAt(),
            revision.resubmittedAt()
        );
    }
}
