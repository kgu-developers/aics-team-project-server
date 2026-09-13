package kgu.developers.admin.midreport.presentation.response;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kgu.developers.domain.midreport.domain.MidReportBlock;
import kgu.developers.domain.midreport.domain.MidReportBlockDefinition;
import kgu.developers.domain.midreport.domain.MidReportBlockStatus;

public record MidReportBlockAdminResponse(
    @Schema(description = "블록 키", example = "gui-design")
    String key,

    @Schema(description = "블록 제목", example = "2. 화면 GUI 설계")
    String title,

    @Schema(description = "블록 상태", example = "COMPLETED")
    MidReportBlockStatus status,

    @Schema(description = "마지막 편집자 학번", example = "202412345")
    String lastEditedBy,

    @Schema(description = "마지막 편집자 이름", example = "홍길동")
    String lastEditedByName,

    @Schema(description = "마지막 저장 일시")
    LocalDateTime lastSavedAt,

    @Schema(description = "블록 필드 데이터 (GUI 블록의 경우 presigned imageUrl 및 imageName 포함)")
    JsonNode fields
) {
    public static MidReportBlockAdminResponse of(
        MidReportBlock block,
        String lastEditedByName,
        JsonNode resolvedFields
    ) {
        MidReportBlockDefinition definition = MidReportBlockDefinition.fromKey(block.getKey());
        return new MidReportBlockAdminResponse(
            block.getKey(),
            definition.title(),
            block.getStatus(),
            block.getLastEditedBy(),
            lastEditedByName,
            block.getLastSavedAt(),
            resolvedFields != null ? resolvedFields : block.getFields()
        );
    }
}
