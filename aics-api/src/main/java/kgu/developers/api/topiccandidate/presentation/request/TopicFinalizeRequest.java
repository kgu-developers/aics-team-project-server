package kgu.developers.api.topiccandidate.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TopicFinalizeRequest(

    @Schema(description = "최종 확정할 주제 후보 식별자", example = "1", requiredMode = REQUIRED)
    @NotNull
    Long candidateId
) {
}
