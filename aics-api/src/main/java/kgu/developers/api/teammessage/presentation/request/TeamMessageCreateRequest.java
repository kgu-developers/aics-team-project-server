package kgu.developers.api.teammessage.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import lombok.Builder;

@Builder
public record TeamMessageCreateRequest(

    @Schema(description = "메시지 관련 유형 (미지정 시 GENERAL)", example = "GENERAL", requiredMode = NOT_REQUIRED)
    TeamMessageRelatedType relatedType,

    @Schema(description = "관련 리소스 id. PROPOSAL이면 projectId, MID_REPORT면 midReportId를 전달하며 두 유형에서는 양수 값이 필수다. 그 외 유형에서는 선택값이다.", example = "1", requiredMode = NOT_REQUIRED)
    Long relatedId,

    @Schema(description = "메시지 본문", example = "다음 회의 일정 문의드립니다.", requiredMode = REQUIRED)
    @NotBlank
    String message
) {
    @JsonIgnore
    @AssertTrue(message = "PROPOSAL과 MID_REPORT 메시지는 양수 relatedId가 필요합니다.")
    public boolean isRelatedIdValid() {
        return relatedType != TeamMessageRelatedType.PROPOSAL && relatedType != TeamMessageRelatedType.MID_REPORT
            || relatedId != null && relatedId > 0;
    }
}
