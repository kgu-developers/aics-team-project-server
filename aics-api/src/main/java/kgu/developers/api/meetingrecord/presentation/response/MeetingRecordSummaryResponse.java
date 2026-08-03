package kgu.developers.api.meetingrecord.presentation.response;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
import kgu.developers.domain.meetingrecord.domain.MeetingRecord;
import lombok.Builder;

@Builder
public record MeetingRecordSummaryResponse(

    @Schema(description = "회의록 식별자", example = "1", requiredMode = REQUIRED)
    Long id,

    @Schema(description = "회의 단계(PROPOSAL:제안, MID_CHECK:중간, FINAL:최종)", example = "MID_CHECK", requiredMode = REQUIRED)
    MeetingPhase phase,

    @Schema(description = "회의 일시", example = "2026-08-03 14:00", requiredMode = REQUIRED)
    String meetingAt,

    @Schema(description = "장소/진행방식", example = "온라인(Zoom)")
    String location,

    @Schema(description = "작성자 학번", example = "202412345", requiredMode = REQUIRED)
    String authorId,

    @Schema(description = "참석자 수", example = "4", requiredMode = REQUIRED)
    int participantCount
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static MeetingRecordSummaryResponse from(MeetingRecord meetingRecord) {
        return MeetingRecordSummaryResponse.builder()
            .id(meetingRecord.getId())
            .phase(meetingRecord.getPhase())
            .meetingAt(meetingRecord.getMeetingAt().format(FORMATTER))
            .location(meetingRecord.getLocation())
            .authorId(meetingRecord.getAuthorId())
            .participantCount(meetingRecord.getParticipantCount())
            .build();
    }
}
