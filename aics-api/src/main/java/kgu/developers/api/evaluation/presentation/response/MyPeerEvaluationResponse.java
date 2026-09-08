package kgu.developers.api.evaluation.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;

public record MyPeerEvaluationResponse(
    Long id,
    String selfContribution,
    String projectReviewComment,
    List<PeerEvaluationAnswerResponse> answers,
    @Schema(allowableValues = {"DRAFT", "SUBMITTED"})
    PeerEvaluationSubmissionStatus status,
    LocalDateTime updatedAt,
    LocalDateTime submittedAt
) {
}
