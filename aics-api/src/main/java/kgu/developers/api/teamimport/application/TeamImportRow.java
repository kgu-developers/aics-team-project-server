package kgu.developers.api.teamimport.application;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.api.importcommon.RowStatus;

public record TeamImportRow(
    @Schema(description = "엑셀 행 번호", example = "2")
    int rowNumber,

    @Schema(description = "팀명", example = "1팀")
    String teamName,

    @Schema(description = "학번", example = "202412345")
    String studentNumber,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "팀장 여부", example = "true")
    boolean leader,

    @Schema(description = "프로젝트 내 역할", example = "백엔드")
    String projectRole,

    @Schema(description = "행 상태", example = "VALID")
    RowStatus status,

    @Schema(description = "상태 사유", example = "이미 이 팀에 편성되어 있습니다.")
    String message
) {
    public TeamImportRow with(RowStatus status, String message) {
        return new TeamImportRow(rowNumber, teamName, studentNumber, name, leader, projectRole, status, message);
    }
}
