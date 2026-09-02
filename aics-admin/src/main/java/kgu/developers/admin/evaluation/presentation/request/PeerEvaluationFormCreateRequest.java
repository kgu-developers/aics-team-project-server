package kgu.developers.admin.evaluation.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PeerEvaluationFormCreateRequest(
        @Schema(description = "마일스톤 ID", example = "1", requiredMode = REQUIRED)
        @NotNull
        @Positive
        Long milestoneId,

        @Schema(description = "평가 결과 익명 공개 여부", example = "true", requiredMode = REQUIRED)
        @NotNull
        Boolean anonymous,

        @Schema(description = "상호평가 시작 시각", example = "2026-10-01T09:00:00", requiredMode = REQUIRED)
        @NotNull
        LocalDateTime opensAt,

        @Schema(description = "상호평가 종료 시각", example = "2026-10-08T23:59:59", requiredMode = REQUIRED)
        @NotNull
        LocalDateTime closesAt
) {
    @JsonIgnore
    @AssertTrue(message = "상호평가 시작 시각은 종료 시각보다 앞서야 합니다.")
    public boolean isPeriodValid() {
        return opensAt == null || closesAt == null || opensAt.isBefore(closesAt);
    }
}
