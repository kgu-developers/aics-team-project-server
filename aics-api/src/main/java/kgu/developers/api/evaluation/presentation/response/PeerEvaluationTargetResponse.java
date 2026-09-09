package kgu.developers.api.evaluation.presentation.response;

import lombok.Builder;

@Builder
public record PeerEvaluationTargetResponse(
    String userId,
    String name,
    String role
) {
}
