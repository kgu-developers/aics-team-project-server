package kgu.developers.api.evaluation.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record PeerEvaluationTargetsResponse(
    Long formId,
    String title,
    @Schema(description = "평가 기간 상태", allowableValues = {"UPCOMING", "OPEN", "CLOSED"})
    String windowState,
    String windowMessage,
    List<PeerEvaluationTargetResponse> targets,
    @Schema(description = "내 임시저장 또는 제출 응답", nullable = true)
    MyPeerEvaluationResponse myResponse
) {
}
