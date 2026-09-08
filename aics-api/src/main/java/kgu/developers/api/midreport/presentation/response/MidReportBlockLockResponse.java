package kgu.developers.api.midreport.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MidReportBlockLockResponse(
    @Schema(description = "편집 잠금 소유자 이름")
    String ownerName
) {
}
