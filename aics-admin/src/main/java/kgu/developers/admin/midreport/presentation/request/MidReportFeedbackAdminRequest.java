package kgu.developers.admin.midreport.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MidReportFeedbackAdminRequest(
    @Schema(description = "피드백 내용", example = "GUI 화면 흐름을 보완하고 예외 처리 계획을 추가해 주세요.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "피드백 내용은 비어 있을 수 없습니다.")
    @Size(max = 2000, message = "피드백 내용은 2000자를 초과할 수 없습니다.")
    String message,

    @Schema(description = "수정이 필요한 블록 키 목록 (선택)", example = "[\"gui-design\", \"engine-design\"]")
    List<String> affectedBlockKeys
) {
}
