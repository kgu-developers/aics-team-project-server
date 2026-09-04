package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record PeerEvaluationFormPersistResponse(
        @Schema(description = "생성된 상호평가 양식 ID", example = "1", requiredMode = REQUIRED)
        Long id
) {
    public static PeerEvaluationFormPersistResponse of(Long id) {
        return new PeerEvaluationFormPersistResponse(id);
    }
}
