package kgu.developers.admin.milestone.presentation.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;

public record MilestoneScheduleResponse(
        @Schema(description = "작성 시작 시각") LocalDateTime opensAt,
        @Schema(description = "제출 마감 시각") LocalDateTime dueAt,
        @Schema(description = "지각 제출 마감 시각") LocalDateTime lateSubmissionUntil,
        @Schema(description = "제출물 수정 마감 시각") LocalDateTime revisionUntil,
        @Schema(description = "평가 시작 시각") LocalDateTime evaluationOpensAt,
        @Schema(description = "평가 종료 시각") LocalDateTime evaluationClosesAt
) {
    public static MilestoneScheduleResponse from(MilestoneSchedule schedule) {
        return new MilestoneScheduleResponse(
                schedule.opensAt(),
                schedule.dueAt(),
                schedule.lateSubmissionUntil(),
                schedule.revisionUntil(),
                schedule.evaluationOpensAt(),
                schedule.evaluationClosesAt()
        );
    }
}
