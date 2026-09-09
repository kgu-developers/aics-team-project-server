package kgu.developers.admin.importstatus.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RosterImportStatusResponse(

    @Schema(description = "수강생 명단 마지막 반영 정보. 성공 반영 이력이 없으면 null입니다.", nullable = true)
    RosterImportAppliedResponse studentRoster,

    @Schema(description = "팀 명단 마지막 반영 정보. 성공 반영 이력이 없으면 null입니다.", nullable = true)
    RosterImportAppliedResponse teamRoster
) {
}
