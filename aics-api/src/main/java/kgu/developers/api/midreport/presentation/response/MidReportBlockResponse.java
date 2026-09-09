package kgu.developers.api.midreport.presentation.response;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kgu.developers.domain.midreport.domain.MidReportBlock;
import kgu.developers.domain.midreport.domain.MidReportBlockDefinition;
import kgu.developers.domain.midreport.domain.MidReportBlockStatus;

public record MidReportBlockResponse(
    @Schema(description = "고정 영역 키", allowableValues = {"topic", "gui-design", "engine-design", "project-plan"})
    String key,
    String title,
    String description,
    JsonNode fields,
    @Schema(description = "영역 작성 상태", allowableValues = {"IN_PROGRESS", "COMPLETED"})
    MidReportBlockStatus status,
    @Schema(description = "편집 잠금. 현재 기반 구현에서는 기존 숫자형 잠금 ID 계약과 맞지 않아 null입니다.", nullable = true)
    MidReportBlockLockResponse lock,
    @Schema(description = "마지막 편집자 학번", nullable = true)
    String lastEditedBy,
    @Schema(description = "마지막 편집자 이름", nullable = true)
    String lastEditedByName,
    LocalDateTime lastSavedAt
) {
    static MidReportBlockResponse from(
        MidReportBlock block,
        String editorName,
        LocalDateTime reportCreatedAt
    ) {
        MidReportBlockDefinition definition = MidReportBlockDefinition.fromKey(block.getKey());
        return new MidReportBlockResponse(
            block.getKey(),
            definition.title(),
            definition.description(),
            block.getFields(),
            block.getStatus(),
            null,
            block.getLastEditedBy(),
            editorName == null ? "" : editorName,
            block.getLastSavedAt() == null ? reportCreatedAt : block.getLastSavedAt()
        );
    }
}
