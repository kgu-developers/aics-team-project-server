package kgu.developers.admin.milestone.presentation.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;

public record MilestoneScheduleRequest(
        @Schema(description = "작성 시작 시각", example = "2026-09-01T00:00:00")
        LocalDateTime opensAt,

        @Schema(description = "제출 마감 시각", example = "2026-09-10T23:59:59", requiredMode = REQUIRED)
        @NotNull
        LocalDateTime dueAt,

        @Schema(description = "지각 제출 마감 시각", example = "2026-09-11T23:59:59")
        LocalDateTime lateSubmissionUntil,

        @Schema(description = "제출물 수정 마감 시각", example = "2026-09-12T23:59:59")
        LocalDateTime revisionUntil,

        @Schema(description = "평가 시작 시각", example = "2026-09-13T00:00:00")
        LocalDateTime evaluationOpensAt,

        @Schema(description = "평가 종료 시각", example = "2026-09-15T23:59:59")
        LocalDateTime evaluationClosesAt
) {
    public MilestoneSchedule toDomain() {
        return new MilestoneSchedule(
                opensAt,
                dueAt,
                lateSubmissionUntil,
                revisionUntil,
                evaluationOpensAt,
                evaluationClosesAt
        );
    }
}
