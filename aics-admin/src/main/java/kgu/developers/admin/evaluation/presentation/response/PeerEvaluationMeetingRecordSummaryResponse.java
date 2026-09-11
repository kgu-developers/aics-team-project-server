package kgu.developers.admin.evaluation.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import lombok.Builder;

@Builder
public record PeerEvaluationMeetingRecordSummaryResponse(
    @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "회의록 제목", example = "OOP-01 - 1팀 프로젝트 킥오프", requiredMode = REQUIRED)
    String title,

    @Schema(description = "회의 단계", example = "MID_CHECK", requiredMode = REQUIRED)
    MeetingPhase phase,

    @Schema(description = "회의 일시", example = "2026-10-01T14:00:00", requiredMode = REQUIRED)
    LocalDateTime meetingAt,

    @Schema(description = "참석자 수", example = "4", requiredMode = REQUIRED)
    int participantCount
) {
    public static PeerEvaluationMeetingRecordSummaryResponse from(MeetingRecord meetingRecord) {
        return PeerEvaluationMeetingRecordSummaryResponse.builder()
            .id(meetingRecord.getId())
            .title(meetingRecord.getTitle())
            .phase(meetingRecord.getPhase())
            .meetingAt(meetingRecord.getMeetingAt())
            .participantCount(meetingRecord.getParticipantCount())
            .build();
    }
}
