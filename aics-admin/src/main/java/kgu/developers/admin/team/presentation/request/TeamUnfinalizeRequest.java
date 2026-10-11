package kgu.developers.admin.team.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamUnfinalizeRequest(
    @Schema(description = "취소 사유(필수, 앞뒤 공백 제거 후 30~500자). 변경된 각 팀의 감사 로그에 기록한다.",
        example = "팀 배정 오류를 수정하고 학생들의 팀 구성을 다시 조정하기 위해 취소합니다.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = 30, max = 500)
    String reason
) {
    public TeamUnfinalizeRequest {
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
