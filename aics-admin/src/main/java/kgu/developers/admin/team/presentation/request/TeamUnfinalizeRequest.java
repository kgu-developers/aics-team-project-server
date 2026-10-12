package kgu.developers.admin.team.presentation.request;

import static kgu.developers.domain.team.domain.Team.MIN_UNFINALIZE_REASON_LENGTH;
import static kgu.developers.domain.team.domain.Team.MAX_UNFINALIZE_REASON_LENGTH;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamUnfinalizeRequest(
    @Schema(description = "취소 사유(필수, 앞뒤 공백 제거 후 " + MIN_UNFINALIZE_REASON_LENGTH
        + "~" + MAX_UNFINALIZE_REASON_LENGTH + "자). 변경된 각 팀의 감사 로그에 기록한다.",
        example = "팀 배정 오류를 수정하고 학생들의 팀 구성을 다시 조정하기 위해 취소합니다.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(min = MIN_UNFINALIZE_REASON_LENGTH, max = MAX_UNFINALIZE_REASON_LENGTH)
    String reason
) {
    public TeamUnfinalizeRequest {
        if (reason != null) {
            reason = reason.trim();
        }
    }
}
