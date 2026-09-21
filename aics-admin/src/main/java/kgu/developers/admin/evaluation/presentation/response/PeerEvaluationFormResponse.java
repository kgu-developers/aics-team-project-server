package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;

public record PeerEvaluationFormResponse(
        @Schema(description = "상호평가 양식 ID", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "분반 ID", example = "1", requiredMode = REQUIRED)
        Long sectionId,

        @Schema(description = "마일스톤 ID", example = "1", requiredMode = REQUIRED)
        Long milestoneId,

        @Schema(description = "평가 결과 익명 공개 여부", example = "true", requiredMode = REQUIRED)
        boolean anonymous,

        @Schema(description = "상호평가 시작 시각", example = "2026-10-01T09:00:00", requiredMode = REQUIRED)
        LocalDateTime opensAt,

        @Schema(description = "상호평가 종료 시각", example = "2026-10-08T23:59:59", requiredMode = REQUIRED)
        LocalDateTime closesAt
) {
    public static PeerEvaluationFormResponse from(PeerEvaluationForm form) {
        if (form == null) {
            return null;
        }
        return new PeerEvaluationFormResponse(
                form.getId(),
                form.getSectionId(),
                form.getMilestoneId(),
                form.isAnonymous(),
                form.getOpensAt(),
                form.getClosesAt()
        );
    }
}
