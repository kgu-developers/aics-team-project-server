package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.evaluation.domain.PeerEvaluationForm;
import lombok.Builder;

@Builder
public record PeerEvaluationAdminListResponse(
    @Schema(description = "분반 식별자", example = "1", requiredMode = REQUIRED)
    Long sectionId,

    @Schema(description = "상호평가 양식 식별자 (양식이 없으면 null)", example = "10")
    Long formId,

    @Schema(description = "평가 시작 일시 (양식이 없으면 null)", example = "2026-12-01T00:00:00")
    LocalDateTime opensAt,

    @Schema(description = "평가 마감 일시 (양식이 없으면 null)", example = "2026-12-15T23:59:59")
    LocalDateTime closesAt,

    @Schema(description = "팀별 상호평가 제출 현황 목록", requiredMode = REQUIRED)
    List<PeerEvaluationAdminTeamSummaryResponse> teams
) {
    public static PeerEvaluationAdminListResponse of(
        Long sectionId,
        PeerEvaluationForm form,
        List<PeerEvaluationAdminTeamSummaryResponse> teams
    ) {
        return PeerEvaluationAdminListResponse.builder()
            .sectionId(sectionId)
            .formId(form != null ? form.getId() : null)
            .opensAt(form != null ? form.getOpensAt() : null)
            .closesAt(form != null ? form.getClosesAt() : null)
            .teams(teams)
            .build();
    }
}
