package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PeerEvaluationMemberResponse(
    @Schema(description = "학번", example = "20260001", requiredMode = REQUIRED)
    String userId,

    @Schema(description = "이름", example = "김민준", requiredMode = REQUIRED)
    String name,

    @Schema(description = "팀장 여부", example = "true", requiredMode = REQUIRED)
    boolean isLeader,

    @Schema(description = "역할 (예: 팀장, 팀원, 프론트엔드 등)", example = "팀장")
    String role,

    @Schema(description = "다른 팀원들로부터 받은 평균 기여도 점수 (제출된 평가 기준, 평가가 없으면 null)", example = "30.0")
    Double averageReceivedScore
) {
}
