package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PeerEvaluationScoreDetailResponse(
    @Schema(description = "피평가자 학번", example = "20260002", requiredMode = REQUIRED)
    String targetUserId,

    @Schema(description = "피평가자 이름", example = "이서연", requiredMode = REQUIRED)
    String targetUserName,

    @Schema(description = "본인 여부 (본인이면 true)", example = "false", requiredMode = REQUIRED)
    boolean isSelf,

    @Schema(description = "부여한 기여도 점수(%) - 본인이거나 미제출 시 null", example = "30")
    Integer contributionPercent
) {
    public static PeerEvaluationScoreDetailResponse self(String userId, String userName) {
        return PeerEvaluationScoreDetailResponse.builder()
            .targetUserId(userId)
            .targetUserName(userName)
            .isSelf(true)
            .contributionPercent(null)
            .build();
    }

    public static PeerEvaluationScoreDetailResponse score(String userId, String userName, Integer score) {
        return PeerEvaluationScoreDetailResponse.builder()
            .targetUserId(userId)
            .targetUserName(userName)
            .isSelf(false)
            .contributionPercent(score)
            .build();
    }
}
