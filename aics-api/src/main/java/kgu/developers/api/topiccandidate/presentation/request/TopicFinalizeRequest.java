package kgu.developers.api.topiccandidate.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TopicFinalizeRequest(

    @Schema(description = "최종 확정할 주제 후보 식별자", example = "1", requiredMode = REQUIRED)
    @NotNull
    @Positive
    Long candidateId,

    @Schema(description = "프로젝트 목표", example = "AI 기반 학습 도우미 개발", requiredMode = REQUIRED)
    @NotNull
    String goal
) {
}
